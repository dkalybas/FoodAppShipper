package com.example.newfoodapp_shipper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import dmax.dialog.SpotsDialog;
import io.paperdb.Paper;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.example.newfoodapp_shipper.Common.Common;
import com.example.newfoodapp_shipper.model.RestaurantModel;
import com.example.newfoodapp_shipper.model.ShipperUserModel;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private  static int APP_REQUEST_CODE = 7171;
    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener listener;
    private AlertDialog dialog;
    private DatabaseReference serverRef;
    private List<AuthUI.IdpConfig> providers;


    @Override
    protected void onStart() {
        super.onStart();
        firebaseAuth.addAuthStateListener(listener);
    }

    @Override
    protected void onStop() {
        if (listener!=null)
            firebaseAuth.removeAuthStateListener(listener);
        super.onStop();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
       init();

    }

    private void init() {

        providers  = Arrays.asList(new AuthUI.IdpConfig.PhoneBuilder().build());

        serverRef = FirebaseDatabase.getInstance().getReference(Common.SHIPPER_REF);
        firebaseAuth  = firebaseAuth.getInstance();

        dialog = new SpotsDialog.Builder().setContext(this).setCancelable(false).build();
        listener = firebaseAuthLocal->{

            FirebaseUser user = firebaseAuthLocal.getCurrentUser();
            if (user!=null){

                Paper.init(this);
                String jsonEncode = Paper.book().read(Common.RESTAURANT_SAVE);
                RestaurantModel restaurantModel = new Gson().fromJson(jsonEncode,new TypeToken<RestaurantModel>(){}.getType());
                if (restaurantModel!=null)
                    checkServerUserFromFirebase(user,restaurantModel);
                else    {
                    startActivity(new Intent(MainActivity.this,RestaurantListActivity.class));
                    finish();
                }


            }else {

                phoneLogin();

            }


        };

    }


    private void checkServerUserFromFirebase(FirebaseUser user, RestaurantModel restaurantModel) {

        dialog.show();

        serverRef = FirebaseDatabase.getInstance().getReference(Common.RESTAURANT_REF)
                .child(restaurantModel.getUid())
                .child(Common.SHIPPER_REF);

        serverRef.child(user.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if(snapshot.exists()){

                            ShipperUserModel userModel = snapshot.getValue(ShipperUserModel.class);
                            if (userModel.isActive()){

                                goToHomeActivity(userModel,restaurantModel);

                            }else{
                                dialog.dismiss();
                                Toast.makeText(MainActivity.this,
                                        "you must be allowed from server app to do that ",Toast.LENGTH_SHORT).show();

                            }

                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });


    }

    private void goToHomeActivity(ShipperUserModel userModel,RestaurantModel restaurantModel) {

        dialog.dismiss();
        Common.currentRestaurant = restaurantModel;
        Common.currentShipperUser = userModel;
        startActivity(new Intent(this,HomeActivity.class));
        finish();


    }


    private void phoneLogin()  {

        startActivityForResult(AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .build(),APP_REQUEST_CODE);


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode==APP_REQUEST_CODE){

            IdpResponse response = IdpResponse .fromResultIntent(data);
            if (resultCode ==RESULT_OK ){

                FirebaseUser user  = FirebaseAuth.getInstance().getCurrentUser();
            }else {

                Toast.makeText(this,"Failed to Sign in ",Toast.LENGTH_SHORT).show();

            }

        }



    }




}
