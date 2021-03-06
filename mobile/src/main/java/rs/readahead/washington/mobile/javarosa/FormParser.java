package rs.readahead.washington.mobile.javarosa;

import android.content.Context;

import com.crashlytics.android.Crashlytics;

import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.FormIndex;
import org.javarosa.form.api.FormEntryCaption;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.form.api.FormEntryPrompt;

import java.util.List;

import rs.readahead.washington.mobile.domain.entity.MediaFile;
import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstance;
import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstanceStatus;
import rs.readahead.washington.mobile.odk.FormController;
import rs.readahead.washington.mobile.odk.exception.JavaRosaException;
import rs.readahead.washington.mobile.util.StringUtils;
import timber.log.Timber;


public class FormParser implements IFormParserContract.IFormParser {
    private IFormParserContract.IView view;
    private FormController formController;
    private Context context;

    private FormEntryPrompt[] prompts;
    private FormEntryCaption[] groups;

    private String startHash;

    private enum Direction {
        PREVIOUS,
        CURRENT,
        NEXT
    }


    public FormParser(IFormParserContract.IView suppliedView) {
        this.view = suppliedView;
        this.formController = FormController.getActive();
        this.context = view.getContext().getApplicationContext();
    }

    @Override
    public void parseForm() {
        init();
        parse(Direction.CURRENT);
    }

    @Override
    public void stepToNextScreen() {
        parse(Direction.NEXT);
    }

    @Override
    public void stepToPrevScreen() {
        parse(Direction.PREVIOUS);
    }

    @Override
    public boolean isFirstScreen() {
        boolean first;

        try {
            first = formController.stepToPreviousScreenEvent() == FormEntryController.EVENT_BEGINNING_OF_FORM;
            formController.stepToNextScreenEvent();

            if (formController.getEvent() == FormEntryController.EVENT_PROMPT_NEW_REPEAT) {
                first = false;
                formController.stepToNextScreenEvent();
            }
        } catch (JavaRosaException e) {
            first = false;
            Timber.e(e, this.getClass().getName());
        }

        return first;
    }

    @Override
    public List<MediaFile> getFormAttachments() {
        return formController.getCollectFormInstance().getMediaFiles();
    }

    @Override
    public boolean isFormChanged() {
        FormDef formDef = FormController.getActive().getFormDef();

        return !StringUtils.isTextEqual(startHash, FormUtils.getFormValuesHash(formDef));
    }

    @Override
    public void startFormChangeTracking() {
        FormDef formDef = FormController.getActive().getFormDef();
        startHash = FormUtils.getFormValuesHash(formDef);
    }

    @Override
    public void executeRepeat() {
        try {
            formController.newRepeat();
        } catch (Exception e) {
            view.formParseError(e);
            return;
        }

        if (!formController.indexIsInFieldList()) {
            stepToNextScreen();
        } else {
            parse(Direction.CURRENT);
        }
    }

    @Override
    public void cancelRepeat() {
        stepToNextScreen();
    }

    @Override
    public void destroy() {
        view = null;
    }

    private void init() {
        boolean enableAttachments = false;
        boolean enableDelete = false;

        FormDef formDef = FormController.getActive().getFormDef();
        FormIndex formIndex = FormUtils.findWhistlerAttachmentFieldIndex(context, formDef);

        if (formIndex != null) {
            enableAttachments = true;
        }

        CollectFormInstance instance = FormController.getActive().getCollectFormInstance();

        if (instance.getStatus().equals(CollectFormInstanceStatus.DRAFT) ||
                instance.getClonedId() > 0) {
            enableDelete = true;
        }

        startFormChangeTracking();

        view.formPropertiesChecked(enableAttachments, enableDelete);
    }

    private void parse(Direction direction) {
        try {
            int event;

            switch (direction) {
                case PREVIOUS:
                    event = formController.stepToPreviousScreenEvent();
                    break;

                case NEXT:
                    event = formController.stepToNextScreenEvent();
                    break;

                default:
                    event = formController.getEvent();
            }

            switch (event) {
                case FormEntryController.EVENT_BEGINNING_OF_FORM:
                    view.formBeginning(formController.getFormTitle());
                    break;

                case FormEntryController.EVENT_END_OF_FORM:
                    view.formEnd(formController.getFormTitle());
                    break;

                case FormEntryController.EVENT_QUESTION:
                    getViewParameters();
                    if (!skipWhistlerAttachmentField(prompts, direction)) {
                        view.formQuestion(prompts, groups);
                    }
                    break;

                case FormEntryController.EVENT_GROUP:
                    getViewParameters();
                    if (!skipWhistlerAttachmentField(prompts, direction)) {
                        view.formGroup(prompts, groups);
                    }
                    break;

                case FormEntryController.EVENT_REPEAT:
                    getViewParameters();
                    if (!skipWhistlerAttachmentField(prompts, direction)) {
                        view.formRepeat(prompts, groups);
                    }
                    break;

                case FormEntryController.EVENT_PROMPT_NEW_REPEAT:
                    view.formPromptNewRepeat(formController.getLastRepeatCount(), formController.getLastGroupText());
                    break;
            }
        } catch (Exception e) {
            viewFormParseError(e);
        }
    }

    private void viewFormParseError(Throwable throwable) {
        Crashlytics.logException(throwable);
        view.formParseError(throwable);
    }

    private void getViewParameters() {
        prompts = formController.getQuestionPrompts();
        groups = formController.getGroupsForCurrentIndex();
    }

    private boolean shouldSkipAttachmentField(FormEntryPrompt[] prompts) {
        boolean skip = false;

        for (FormEntryPrompt p: prompts) {
            if (! FormUtils.isWhistlerAttachmentField(context, p)) {
                return false;
            } else {
                skip = true;
            }
        }

        return skip;
    }

    private boolean skipWhistlerAttachmentField(FormEntryPrompt[] prompts, Direction direction) {
        if (shouldSkipAttachmentField(prompts)) {
            if (direction == Direction.PREVIOUS) {
                stepToPrevScreen();
            } else {
                stepToNextScreen();
            }
            return true;
        }

        return false;
    }
}
