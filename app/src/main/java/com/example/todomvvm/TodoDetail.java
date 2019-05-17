package com.example.todomvvm;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v4.app.ShareCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.example.todomvvm.database.AppDatabase;
import com.example.todomvvm.database.Todo;

import static com.example.todomvvm.AddTodoActivity.INSTANCE_TASK_ID;

public class TodoDetail extends AppCompatActivity {

    private static final int DEFAULT_TODO_ID = -1;
    private int mTodoId = DEFAULT_TODO_ID;
    public static final String EXTRA_TASK_ID = "extraTaskId";
    // Extra for the task ID to be received after rotation
    public static final String INSTANCE_TASK_ID = "instanceTaskId";
    private AppDatabase mDb;

    TextView todo_title;
    TextView description;
    TextView calendar;
    TextView clock;
    TextView txt_share;
    TextView txt_date, txt_time;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_todo_detail);

        todo_title = findViewById(R.id.todo_title);
        description = findViewById(R.id.description);
        txt_date = findViewById(R.id.txt_date);
        txt_time = findViewById(R.id.txt_time);
        txt_share = findViewById(R.id.share_todo);


        if (savedInstanceState != null && savedInstanceState.containsKey(INSTANCE_TASK_ID)) {
            mTodoId = savedInstanceState.getInt(INSTANCE_TASK_ID, DEFAULT_TODO_ID);
        }
        mDb = AppDatabase.getInstance(getApplicationContext());


        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(EXTRA_TASK_ID)) {
            if (mTodoId == DEFAULT_TODO_ID) {
                // populate the UI
                mTodoId = intent.getIntExtra(EXTRA_TASK_ID, DEFAULT_TODO_ID);

                AddTodoViewModelFactory factory = new AddTodoViewModelFactory(mDb, mTodoId);
                final AddTodoViewModel viewModel = ViewModelProviders.of(this, factory).get(AddTodoViewModel.class);

                viewModel.getTodo().observe(TodoDetail.this, new Observer<Todo>() {
                    @Override
                    public void onChanged(@Nullable Todo todoEntry) {
                        viewModel.getTodo().removeObserver(this);
                        todo_title.setText(todoEntry.getTitle());
                        description.setText(todoEntry.getDescription());
                        txt_date.setText(todoEntry.getDate());
                        txt_time.setText(todoEntry.getTime());
                        //calendar.setText(todoEntry.getCalendar());
                        //clock.setText(todoEntry.getClock());
                    }
                });


            }
        }

        txt_share.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
              String shareText =  todo_title.getText().toString();
                ShareCompat.IntentBuilder
                        .from(TodoDetail.this)
                        .setType("text/plain")
                        .setChooserTitle("Choose application to share")
                        .setText(shareText)
                        .startChooser();
            }
        });
    }
}
