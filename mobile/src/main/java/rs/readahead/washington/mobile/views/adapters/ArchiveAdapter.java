package rs.readahead.washington.mobile.views.adapters;

import android.content.Context;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.models.Report;
import rs.readahead.washington.mobile.util.DateUtil;
import rs.readahead.washington.mobile.util.DialogsUtil;
import rs.readahead.washington.mobile.views.fragment.ArchiveListFragment;

public class ArchiveAdapter extends RecyclerView.Adapter<ArchiveAdapter.ViewHolder> {

    private final List<Report> mValues;
    private final ArchiveListFragment.OnSendInteractionListener mSendListener;
    private final ArchiveListFragment.OnPreviewInteractionListener mPreviewListener;
    private final ArchiveListFragment.OnDeleteInteractionListener mDeleteListener;
    private final Context context;
    private AlertDialog mRemoveDialog;

    public ArchiveAdapter(List<Report> items, ArchiveListFragment.OnSendInteractionListener sendListener,
                          ArchiveListFragment.OnPreviewInteractionListener previewListener,
                          ArchiveListFragment.OnDeleteInteractionListener deleteListener, Context context) {
        mValues = items;
        mPreviewListener = previewListener;
        mSendListener = sendListener;
        mDeleteListener = deleteListener;
        this.context = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.archive_report_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        final Report report = mValues.get(position);
        holder.mTitle.setText(report.getTitle());
        holder.mDate.setText(DateUtil.getStringFromDate(report.getDate()));

        holder.mResend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != mSendListener) {
                    mSendListener.onSendInteraction(report);
                }
            }
        });

        holder.mDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDraftDeleteDialog(report.getTitle(), position);
            }
        });

        holder.mLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPreviewListener.onPreviewInteraction(report);
            }
        });
    }

    private void deleteReport(int position) {
        mDeleteListener.onDeleteFragmentInteraction(mValues.get(position));
        mValues.remove(position);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, getItemCount());

    }

    private void showDraftDeleteDialog(String title, final int position) {
        mRemoveDialog = DialogsUtil.showRemoveReportDialog(title, context, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteReport(position);
                mRemoveDialog.dismiss();
            }
        });
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.report_title) TextView mTitle;
        @BindView(R.id.report_date) TextView mDate;
        @BindView(R.id.send_report) ImageView mResend;
        @BindView(R.id.delete_report) ImageView mDelete;
        @BindView(R.id.report_layout) LinearLayout mLayout;

        ViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, itemView);
        }

    }
}
