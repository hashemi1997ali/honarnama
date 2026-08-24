package ir.hashemi.market;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import ir.hashemi.market.connection.API;
import ir.hashemi.market.connection.RestAdapter;
import ir.hashemi.market.connection.callbacks.CallbackUser;
import ir.hashemi.market.data.SharedPref;
import ir.hashemi.market.model.User;
import ir.hashemi.market.utils.CallbackDialog;
import ir.hashemi.market.utils.DialogUtils;
import ir.hashemi.market.utils.NetworkCheck;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ActivityRegister extends AppCompatActivity {

    private SharedPref sharedPref;
    private EditText name;
    private EditText username;
    private EditText password;
    private Button register;
    private TextView login_page;

    static ir.hashemi.market.ActivityRegister activityRegister;

    private Call<CallbackUser> callbackCall = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        activityRegister = this;

        sharedPref = new SharedPref(this);

        initComponent();
    }

    private void initComponent() {
        name = (EditText) findViewById(R.id.register_name);
        username = (EditText) findViewById(R.id.register_username);
        password = (EditText) findViewById(R.id.register_password);
        register = (Button) findViewById(R.id.register_button);
        login_page = (TextView) findViewById(R.id.login_page_button);
        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                registerClick();
            }
        });
        login_page.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getApplicationContext(), ir.hashemi.market.ActivityLogin.class);
                startActivity(i);
                finish();
            }
        });

        password.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if(i== EditorInfo.IME_ACTION_DONE){
                    registerClick();
                }
                return false;
            }
        });
    }

    private void registerClick() {
        User user = new User(
                name.getText().toString(),
                username.getText().toString(),
                password.getText().toString()
        );
        API api = RestAdapter.createAPI();
        callbackCall = api.registerUser(user);
        callbackCall.enqueue(new Callback<CallbackUser>() {
            @Override
            public void onResponse(Call<CallbackUser> call, Response<CallbackUser> response) {
                CallbackUser resp = response.body();
                if (response.isSuccessful() && resp != null && "success".equals(resp.status) && resp.data != null) {
                    User user = resp.data;
                    sharedPref.setUserData(user);
                    Toast.makeText(activityRegister, user.name + " " + getString(R.string.welcome), Toast.LENGTH_SHORT).show();
                    startActivityMainDelay();
                } else {
                    String message = resp != null && resp.msg != null && !resp.msg.isEmpty()
                            ? resp.msg
                            : getString(R.string.msg_failed_load_data);
                    Toast.makeText(activityRegister, message, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<CallbackUser> call, Throwable t) {
                Log.e("ActivityRegister", "Registration request failed", t);
                if (!call.isCanceled()) onFailRequest();
            }
        });
    }

    private void startActivityMainDelay() {
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent i = new Intent(ir.hashemi.market.ActivityRegister.this, ir.hashemi.market.ActivityMain.class);
                startActivity(i);
                finish();
            }
        }, 600);
    }

    private void onFailRequest() {
        if (NetworkCheck.isConnect(this)) {
            dialogServerNotConnect();
        } else {
            dialogNoInternet();
        }
    }

    public void dialogServerNotConnect() {
        Dialog dialog = new DialogUtils(this).buildDialogWarning(R.string.title_unable_connect, R.string.msg_unable_connect, R.string.CLOSE, R.drawable.img_no_connect, new CallbackDialog() {
            @Override
            public void onPositiveClick(Dialog dialog) {
                dialog.dismiss();
            }

            @Override
            public void onNegativeClick(Dialog dialog) {
            }
        });
        dialog.show();
    }

    public void dialogNoInternet() {
        Dialog dialog = new DialogUtils(this).buildDialogWarning(R.string.title_no_internet, R.string.msg_no_internet, R.string.CLOSE, R.drawable.img_no_internet, new CallbackDialog() {

            @Override
            public void onPositiveClick(Dialog dialog) {
                finish();
            }

            @Override
            public void onNegativeClick(Dialog dialog) {
            }
        });
        dialog.show();
    }
}
