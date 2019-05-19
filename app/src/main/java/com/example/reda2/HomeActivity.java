 package com.example.reda2;

import android.app.ProgressDialog;
import android.content.Intent;

import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.Auth;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthCredential;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import java.util.Arrays;

 public class HomeActivity extends AppCompatActivity{

     private EditText user, pass;
     private Button login;
     private TextView text;
     private ProgressDialog progressdialog;
     private FirebaseAuth firebaseAuth;
     GoogleSignInClient mGoogleSignInClient;
     SignInButton google_signin;
     LoginButton face_login;
     CallbackManager callbackManager;

     @Override
     protected void onCreate(Bundle savedInstanceState) {
         super.onCreate(savedInstanceState);
         setContentView(R.layout.activity_home);

         progressdialog = new ProgressDialog(this);

         FacebookSdk.sdkInitialize(getApplicationContext());
         face_login=(LoginButton) findViewById(R.id.facebook_login);
         callbackManager = CallbackManager.Factory.create();
         face_login.setReadPermissions(Arrays.asList("email"));

        google_signin = (SignInButton) findViewById(R.id.google_sign_in);
         google_signin.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View v) {
                 Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                 startActivityForResult(signInIntent, 101);
             }
         });


         login = (Button) findViewById(R.id.btnlogin);
         user = (EditText) findViewById(R.id.txtemail);
         pass = (EditText) findViewById(R.id.txtpassword);

         text = (TextView) findViewById(R.id.registerlink);
         text.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View v) {
                 Intent m = new Intent(HomeActivity.this, Register.class);
                 startActivity(m);
                 return;
             }
         });


         GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                 .requestIdToken(getString(R.string.default_web_client_id))
                 .requestEmail()
                 .build();
         mGoogleSignInClient = GoogleSignIn.getClient(this,gso);


         firebaseAuth = FirebaseAuth.getInstance();
         if (firebaseAuth.getCurrentUser() != null) {
             //profile activity here
             finish();
             startActivity(new Intent(getApplicationContext(), ProfileActivity.class));
         }

         login.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View v) {
                 String email = user.getText().toString().trim();
                 String password = pass.getText().toString().trim();

                 if (TextUtils.isEmpty(email)) {
                     Toast.makeText(HomeActivity.this, "Please insert email", Toast.LENGTH_SHORT).show();
                     return;
                 }
                 if (TextUtils.isEmpty(password)) {
                     Toast.makeText(HomeActivity.this, "Please enter Password", Toast.LENGTH_SHORT).show();
                     return;
                 }
                 progressdialog.setMessage("Logging In... Please wait");
                 progressdialog.show();

                 firebaseAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(HomeActivity.this, new OnCompleteListener<AuthResult>() {
                     @Override
                     public void onComplete(@NonNull Task<AuthResult> task) {
                         progressdialog.dismiss();

                         if (task.isSuccessful()) {
                             //start profile activity

                             startActivity(new Intent(getApplicationContext(), ProfileActivity.class));
                             finish();
                         } else {
                             Toast.makeText(HomeActivity.this, "Failed to Login", Toast.LENGTH_SHORT).show();
                         }
                     }
                 });
             }
         });
     }

     public void buttonclickLoginFb(View v){

         LoginManager.getInstance().registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
             @Override
             public void onSuccess(LoginResult loginResult) {
            handleFacebookToken(loginResult.getAccessToken());
             }

             @Override
             public void onCancel() {
                 Toast.makeText(getApplicationContext(), "Operation cancelled", Toast.LENGTH_SHORT).show();
             }

             @Override
             public void onError(FacebookException error) {
                 Toast.makeText(getApplicationContext(), error.getMessage(), Toast.LENGTH_SHORT).show();
             }
         });
     }

     private void handleFacebookToken(AccessToken accessToken) {

         AuthCredential credential = FacebookAuthProvider.getCredential(accessToken.getToken());
         firebaseAuth.signInWithCredential(credential).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
             @Override
             public void onComplete(@NonNull Task<AuthResult> task) {
                 if(task.isSuccessful()){
                     startActivity(new Intent(getApplicationContext(), ProfileActivity.class));
                     finish();
                 }
                 else{
                     Toast.makeText(getApplicationContext(), "Could not register to firebase", Toast.LENGTH_LONG).show();
                 }
             }
         });
     }


     private void updateUI(FirebaseUser myuser) {
         user.setText(myuser.getEmail());

     }


     @Override
     public void onActivityResult(int requestCode, int resultCode, Intent data) {
         callbackManager.onActivityResult(requestCode, resultCode, data);
         super.onActivityResult(requestCode, resultCode, data);

         // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
         if (requestCode == 101) {
             Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
             try {
                 // Google Sign In was successful, authenticate with Firebase
                 GoogleSignInAccount account = task.getResult(ApiException.class);
                 firebaseAuthWithGoogle(account);
             } catch (ApiException e) {
                 // Google Sign In failed, update UI appropriately
                
                 // ...
             }
         }
     }

     private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
         AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
         firebaseAuth.signInWithCredential(credential)
                 .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                     @Override
                     public void onComplete(@NonNull Task<AuthResult> task) {
                         if (task.isSuccessful()) {
                             // Sign in success, update UI with the signed-in user's information

                             FirebaseUser user = firebaseAuth.getCurrentUser();

                             Intent i = new Intent(getApplicationContext(), ProfileActivity.class);
                             startActivity(i);
                             finish();
                             Toast.makeText(getApplicationContext(), "Google sign in successful", Toast.LENGTH_SHORT).show();

                         } else {
                             // If sign in fails, display a message to the user.
                             Toast.makeText(getApplicationContext(), "Google sign in failed", Toast.LENGTH_SHORT).show();
                         }

                         // ...
                     }
                 });

     }







     }


