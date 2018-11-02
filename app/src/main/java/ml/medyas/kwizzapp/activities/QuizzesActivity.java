package ml.medyas.kwizzapp.activities;

import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import ml.medyas.kwizzapp.R;
import ml.medyas.kwizzapp.classes.CategoryItemClass;
import ml.medyas.kwizzapp.classes.OpentDBCalls;
import ml.medyas.kwizzapp.classes.OpentDBClass;
import ml.medyas.kwizzapp.classes.QuestionClass;
import ml.medyas.kwizzapp.classes.UserCoins;
import ml.medyas.kwizzapp.fragments.QuizQuestionFragment;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static ml.medyas.kwizzapp.activities.MainActivity.CATEGORY_POSITION;
import static ml.medyas.kwizzapp.activities.MainActivity.QUESTION_DIFF;
import static ml.medyas.kwizzapp.activities.MainActivity.QUESTION_NUM;
import static ml.medyas.kwizzapp.classes.UtilsClass.getCategories;

public class QuizzesActivity extends AppCompatActivity implements QuizQuestionFragment.QuizQuestionInterface{
    @BindView(R.id.question_container) FrameLayout container;
    @BindView(R.id.question_toolbar) Toolbar toolbar;
    @BindView(R.id.content_failed) LinearLayout failedLayout;
    @BindView(R.id.content_loading) LinearLayout loadingLayout;
    @BindView(R.id.content_start) Button startQuiz;

    private int questionPosition = 0;
    private List<QuestionClass> questionsList = new ArrayList<QuestionClass>();

    private FirebaseUser user;
    private DatabaseReference mDatabase;

    private int position = 0;
    private boolean loaded = false;
    private long coinsCount = 0;
    private UserCoins userCoins;
    private String difficulty;
    private String questions;
    private CategoryItemClass categoryItemClass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quizzes);
        ButterKnife.bind(this);

        user = FirebaseAuth.getInstance().getCurrentUser();
        mDatabase = FirebaseDatabase.getInstance().getReference("usersCoins").child(user.getUid());
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                userCoins = dataSnapshot.getValue(UserCoins.class);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        if(getIntent() != null) {
            Bundle bundle =  getIntent().getExtras();
            position = bundle.getInt(CATEGORY_POSITION);
            difficulty = bundle.getString(QUESTION_DIFF);
            questions = bundle.getString(QUESTION_NUM);
        }
        if(savedInstanceState != null) {
            position = savedInstanceState.getInt(CATEGORY_POSITION);
            difficulty = savedInstanceState.getString(QUESTION_DIFF);
            questions = savedInstanceState.getString(QUESTION_NUM);
            questionsList = savedInstanceState.getParcelableArrayList("quizzes");
            loaded = savedInstanceState.getBoolean("loadedData");
        }

        if(loaded) {
            loadingLayout.setVisibility(View.GONE);
            container.setVisibility(View.VISIBLE);
        }

        categoryItemClass = getCategories().get(position);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_arrow_back_black_24dp);

        if (savedInstanceState == null) {
            if(difficulty.equals("any") && categoryItemClass.getIndex().equals("any")) {
                new OpentDBCalls().getQuestion(questions, "multiple").enqueue(new Callback<OpentDBClass>() {
                    @Override
                    public void onResponse(Call<OpentDBClass> call, Response<OpentDBClass> response) {
                        processQuestions(response.body().getResults());
                    }

                    @Override
                    public void onFailure(Call<OpentDBClass> call, Throwable t) {
                        processQuestions(null);
                    }
                });
            } else if(difficulty.equals("any")) {
                new OpentDBCalls().getQuestionByCategory(questions, categoryItemClass.getIndex(), "multiple").enqueue(new Callback<OpentDBClass>() {
                    @Override
                    public void onResponse(Call<OpentDBClass> call, Response<OpentDBClass> response) {
                        processQuestions(response.body().getResults());
                    }

                    @Override
                    public void onFailure(Call<OpentDBClass> call, Throwable t) {
                        processQuestions(null);
                    }
                });
            } else if(categoryItemClass.getIndex().equals("any")) {
                new OpentDBCalls().getQuestionByDiff(questions, difficulty,"multiple").enqueue(new Callback<OpentDBClass>() {
                    @Override
                    public void onResponse(Call<OpentDBClass> call, Response<OpentDBClass> response) {
                        processQuestions(response.body().getResults());
                    }

                    @Override
                    public void onFailure(Call<OpentDBClass> call, Throwable t) {
                        processQuestions(null);
                    }
                });
            } else {
                new OpentDBCalls().getQuestionAll(questions, categoryItemClass.getIndex(), difficulty, "multiple").enqueue(new Callback<OpentDBClass>() {
                    @Override
                    public void onResponse(Call<OpentDBClass> call, Response<OpentDBClass> response) {
                        processQuestions(response.body().getResults());
                    }

                    @Override
                    public void onFailure(Call<OpentDBClass> call, Throwable t) {
                        processQuestions(null);
                    }
                });
            }
        }

        getSupportActionBar().setTitle(categoryItemClass.getName());

        startQuiz.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startQuiz.setVisibility(View.GONE);
                container.setVisibility(View.VISIBLE);
                replaceFragment();
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(QUESTION_DIFF, difficulty);
        outState.putString(QUESTION_NUM, questions);
        outState.putInt(CATEGORY_POSITION, position);
        outState.putBoolean("loadedData", loaded);
        outState.putParcelableArrayList("quizzes", (ArrayList<? extends Parcelable>) questionsList);
    }

    @Override
    public void onBackPressed() {
        if(questionPosition < questionsList.size()) {
            quitDialog().show();
        } else {
            updateUserCoins();
            super.onBackPressed();
        }
    }

    public AlertDialog quitDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("The quiz is still running, Are you sure you want to exit?");
        // Add the buttons
        builder.setPositiveButton("Exit", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
                questionPosition = questionsList.size();
                onBackPressed();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
                dialog.dismiss();
            }
        });

        // Create the AlertDialog
        return builder.create();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }

    private void processQuestions(List<QuestionClass> results) {
        loaded = true;
        if ( results == null || results.isEmpty()) {
            failedLayout.setVisibility(View.VISIBLE);
            loadingLayout.setVisibility(View.GONE);
            Toast.makeText(this, "Found Empty Data!", Toast.LENGTH_SHORT).show();
            return;
        }

        loadingLayout.setVisibility(View.GONE);
        startQuiz.setVisibility(View.VISIBLE);
        questionsList.addAll(results);

    }

    @Override
    public void onFinished(boolean correct) {
        if (correct) {
            coinsCount++;
        }
        if(questionPosition < questionsList.size()) {
            replaceFragment();
        } else {
            Toast.makeText(this, "Completed the Quiz", Toast.LENGTH_SHORT).show();
            updateUserCoins();
            onBackPressed();
        }
    }

    private void updateUserCoins() {
        UserCoins u = new UserCoins(user.getUid(), (userCoins.getUserCoins()+coinsCount));
        mDatabase.setValue(u);
    }

    public void replaceFragment() {
        Fragment frag = new QuizQuestionFragment();
        Bundle bundle = new Bundle();
        QuestionClass quiz = questionsList.get(0);
        Log.d("QuizFragment", quiz.getQuestion()+" " +quiz.getCorrect_answer()+" "+ quiz.getIncorrect_answers().length );
        bundle.putParcelable("question", questionsList.get(questionPosition));
        frag.setArguments(bundle);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.question_container, frag)
                .commit();

        getSupportActionBar().setSubtitle(String.format("Question %d/%d", questionPosition+1, questionsList.size()));
        questionPosition++;
    }
}
