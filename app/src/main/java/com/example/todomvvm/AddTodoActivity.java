package com.example.todomvvm;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TimePicker;

import com.example.todomvvm.database.AppDatabase;
import com.example.todomvvm.database.Todo;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class AddTodoActivity extends AppCompatActivity {

    // Extra for the task ID to be received in the intent
    public static final String EXTRA_TASK_ID = "extraTaskId";
    // Extra for the task ID to be received after rotation
    public static final String INSTANCE_TASK_ID = "instanceTaskId";
    // Constants for priority
    public static final int PRIORITY_HIGH = 1;
    public static final int PRIORITY_MEDIUM = 2;
    public static final int PRIORITY_LOW = 3;
    // Constant for default task id to be used when not in update mode
    private static final int DEFAULT_TODO_ID = -1;
    // Constant for logging
    private static final String TAG = AddTodoActivity.class.getSimpleName();
    // Fields for views
    EditText mEditText;
    EditText todo_date;
    EditText todo_time;
    EditText todo_title;
    RadioGroup mRadioGroup;
    Button mButton;

    private int mTodoId = DEFAULT_TODO_ID;

    // Member variable for the Database
    private AppDatabase mDb;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_todo);

        initViews();

        mDb = AppDatabase.getInstance(getApplicationContext());

        if (savedInstanceState != null && savedInstanceState.containsKey(INSTANCE_TASK_ID)) {
            mTodoId = savedInstanceState.getInt(INSTANCE_TASK_ID, DEFAULT_TODO_ID);
        }

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(EXTRA_TASK_ID)) {
            mButton.setText(R.string.update_button);
            if (mTodoId == DEFAULT_TODO_ID) {
                // populate the UI
                mTodoId = intent.getIntExtra(EXTRA_TASK_ID, DEFAULT_TODO_ID);

                AddTodoViewModelFactory factory = new AddTodoViewModelFactory(mDb, mTodoId);
                final AddTodoViewModel viewModel = ViewModelProviders.of(this, factory).get(AddTodoViewModel.class);

                viewModel.getTodo().observe(AddTodoActivity.this, new Observer<Todo>() {
                    @Override
                    public void onChanged(@Nullable Todo todoEntry) {
                        viewModel.getTodo().removeObserver(this);
                        Log.d(TAG, "Receiving database update from LiveData");
                        populateUI(todoEntry);
                    }
                });


            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(INSTANCE_TASK_ID, mTodoId);
        super.onSaveInstanceState(outState);
    }

    /**
     * initViews is called from onCreate to init the member variable views
     */
    private void initViews() {
        mEditText = findViewById(R.id.editTextTaskDescription);
        mRadioGroup = findViewById(R.id.radioGroup);
        todo_date = findViewById(R.id.todo_date);
        todo_time = findViewById(R.id.todo_time);
        todo_title = findViewById(R.id.todo_title);

        final Calendar myCalendar = Calendar.getInstance();

        final DatePickerDialog.OnDateSetListener date = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                myCalendar.set(Calendar.YEAR, year);
                myCalendar.set(Calendar.MONTH, month);
                myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                String myFormat = "MM/dd/yy";
                SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.US);
                todo_date.setText(sdf.format(myCalendar.getTime()));
            }
        };

        todo_date.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                new DatePickerDialog(AddTodoActivity.this, date, myCalendar.get(Calendar.YEAR), myCalendar.get(Calendar.MONTH), myCalendar.get(Calendar.DAY_OF_MONTH)).show();

            }
        });

        todo_time.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Calendar currentTime = Calendar.getInstance();
                int hour = currentTime.get(Calendar.HOUR_OF_DAY);
                int minute = currentTime.get(Calendar.MINUTE);
                TimePickerDialog timePicker;

                timePicker = new TimePickerDialog(AddTodoActivity.this, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        todo_time.setText(hourOfDay + ":" + minute);
                    }
                }, hour, minute, false);

                timePicker.setTitle("Select Time");
                timePicker.show();

            }
        });
        mButton = findViewById(R.id.saveButton);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onSaveButtonClicked();
            }
        });
    }

    /**
     * populateUI would be called to populate the UI when in update mode
     *
     * @param todo the todo to populate the UI
     */
    private void populateUI(Todo todo) {
        if (todo == null)
            return;
        mEditText.setText(todo.getDescription());
        todo_title.setText(todo.getTitle());
        todo_date.setText(todo.getDate());
        todo_time.setText(todo.getTime());
        setPriorityInViews(todo.getPriority());
    }

    /**
     * onSaveButtonClicked is called when the "save" button is clicked.
     * It retrieves user input and inserts that new task data into the underlying database.
     */
    public void onSaveButtonClicked() {
        String description = mEditText.getText().toString();
        int priority = getPriorityFromViews();
        String tododate = todo_date.getText().toString();
        String todotime = todo_time.getText().toString();
        String title = todo_title.getText().toString();
        Date date = new Date();

        // TODO (4) Make todo final so it is visible inside the run method
        final Todo todo = new Todo(title, description, priority, date, tododate, todotime);
        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                if (mTodoId == DEFAULT_TODO_ID)
                    mDb.todoDao().insertTodo(todo);
                else {
                    todo.setId(mTodoId);
                    mDb.todoDao().update(todo);
                }
            }
        });
        // TODO (2) Get the diskIO Executor from the instance of AppExecutors and
        // call the diskIO execute method with a new Runnable and implement its run method
        // TODO (3) Move the remaining logic inside the run method

        finish();
    }

    /**
     * getPriority is called whenever the selected priority needs to be retrieved
     */
    public int getPriorityFromViews() {
        int priority = 1;
        int checkedId = ((RadioGroup) findViewById(R.id.radioGroup)).getCheckedRadioButtonId();
        switch (checkedId) {
            case R.id.radButton1:
                priority = PRIORITY_HIGH;
                break;
            case R.id.radButton2:
                priority = PRIORITY_MEDIUM;
                break;
            case R.id.radButton3:
                priority = PRIORITY_LOW;
        }
        return priority;
    }

    /**
     * setPriority is called when we receive a task from MainActivity
     *
     * @param priority the priority value
     */
    public void setPriorityInViews(int priority) {
        switch (priority) {
            case PRIORITY_HIGH:
                ((RadioGroup) findViewById(R.id.radioGroup)).check(R.id.radButton1);
                break;
            case PRIORITY_MEDIUM:
                ((RadioGroup) findViewById(R.id.radioGroup)).check(R.id.radButton2);
                break;
            case PRIORITY_LOW:
                ((RadioGroup) findViewById(R.id.radioGroup)).check(R.id.radButton3);
        }
    }
}
