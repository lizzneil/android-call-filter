package com.novyr.callfilter.ui.loglist;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import com.novyr.callfilter.ContactFinder;
import com.novyr.callfilter.R;
import com.novyr.callfilter.formatter.LogDateFormatter;
import com.novyr.callfilter.formatter.LogMessageFormatter;
import com.novyr.callfilter.permissions.PermissionChecker;
import com.novyr.callfilter.ui.rulelist.RuleListActivity;
import com.novyr.callfilter.viewmodel.LogViewModel;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class LogListActivity extends AppCompatActivity {
    private RecyclerView mLogList;
    private LogViewModel mLogViewModel;
//    private Snackbar mPermissionNotice;
    androidx.appcompat.app.AlertDialog mPermissionNotice;
    private PermissionChecker mPermissionChecker;

    private static final String TAG = LogListActivity.class.getSimpleName();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_log_list);

        mLogList = findViewById(R.id.log_list);

        mLogViewModel = new ViewModelProvider(this).get(LogViewModel.class);

        final ContactFinder contactFinder = new ContactFinder(this);
        final LogListMenuHandler menuHandler = new LogListMenuHandler(
                this,
                contactFinder,
                mLogViewModel
        );

        final LogListAdapter adapter = new LogListAdapter(
                this,
                new LogMessageFormatter(getResources(), contactFinder),
                new LogDateFormatter(),
                menuHandler
        );

        mLogList.setAdapter(adapter);
        mLogList.setLayoutManager(new LinearLayoutManager(this));

        final TextView emptyView = findViewById(R.id.empty_view);

        mLogViewModel.findAll().observe(this, entities -> {
            adapter.setEntities(entities);

            if (adapter.getItemCount() > 0) {
                mLogList.setVisibility(View.VISIBLE);
                emptyView.setVisibility(View.GONE);
            } else {
                mLogList.setVisibility(View.GONE);
                emptyView.setVisibility(View.VISIBLE);
            }
        });

        mPermissionChecker = new PermissionChecker(this, errors -> {
            if (mPermissionNotice != null) {
                mPermissionNotice.dismiss();
                mPermissionNotice = null;
            }

            if (errors.size() < 1) {
                return;
            }

            StringBuilder errorMessage = new StringBuilder();
            for (int i = 0; i < errors.size(); i++) {
                if (errorMessage.length() > 0) {
                    errorMessage.append("\n");
                }
                errorMessage.append(errors.get(i));
            }

//            mPermissionNotice = Snackbar.make(mLogList, errorMessage, Snackbar.LENGTH_INDEFINITE);
//            mPermissionNotice
//                    .setAction(
//                            R.string.permission_notice_retry,
//                            view -> mPermissionChecker.onStart()
//                    )
//                    .show();


            // Use the Builder class for convenient dialog construction
            androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
            errorMessage.append(this.getString(R.string.user_agreement_tips));
            builder.setMessage(errorMessage.toString())
                    .setPositiveButton(R.string.agree, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // START THE GAME!
                            mPermissionChecker.onStart();
                        }
                    })
                    .setNegativeButton(R.string.reject, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // User cancelled the dialog

                        }
                    });
            // Create the AlertDialog object and return it
            androidx.appcompat.app.AlertDialog dialog =  builder.create();
            mPermissionNotice  = dialog;
            dialog.show();
        });

        Observable.create(
                        //The work you need to do
                new ObservableOnSubscribe<Integer>() {
                    @Override
                    public void subscribe(@NonNull ObservableEmitter<Integer> e) throws Exception {
                        Log.e(TAG, "Observable thread is : " + Thread.currentThread().getName());
                        e.onNext(1);
                        e.onComplete();
                    }
                }).subscribeOn(Schedulers.newThread())//thread you need the work to perform on
                .subscribeOn(Schedulers.io())//thread you need the work to perform on
                 .observeOn(AndroidSchedulers.mainThread())////thread you need to handle the result on
                .doOnNext(new Consumer<Integer>() {
                    @Override
                    public void accept(@NonNull Integer integer) throws Exception {
                        Log.e(TAG, "After observeOn(mainThread)，Current thread is " + Thread.currentThread().getName());
                    }
                })
                .observeOn(Schedulers.io())
                .subscribe(
                        //handle the result here
                        new Consumer<Integer>() {
                    @Override
                    public void accept(@NonNull Integer integer) throws Exception {
                        Log.e(TAG, "After observeOn(io)，Current thread is " + Thread.currentThread().getName());
                    }
                });
    }

    @Override
    protected void onStart() {
        super.onStart();

        mPermissionChecker.onStart();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_log_viewer, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_rules) {
            startActivity(new Intent(this, RuleListActivity.class));
            return true;
        } else if (itemId == R.id.action_clear_log) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.dialog_clear_logs_title)
                    .setMessage(R.string.dialog_clear_logs_message)
                    .setIconAttribute(android.R.attr.alertDialogIcon)
                    .setPositiveButton(
                            R.string.yes,
                            (dialog, whichButton) -> mLogViewModel.deleteAll()
                    )
                    .setNegativeButton(R.string.no, null)
                    .show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        mPermissionChecker.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        mPermissionChecker.onActivityResult(requestCode, resultCode, data);
    }
}
