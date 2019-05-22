package manika.aditya.ekayana.Share;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;

import manika.aditya.ekayana.R;
import manika.aditya.ekayana.Utils.FirebaseMethods;
import manika.aditya.ekayana.Utils.UniversalImageLoader;
import manika.aditya.ekayana.models.User;

import static manika.aditya.ekayana.Utils.ImageManager.getBytesFromBitmap;

/**
 * Created by User on 7/24/2017.
 */

public class NextActivity extends AppCompatActivity {

    private static final String TAG = "NextActivity";


    //firebase
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference myRef;
    private FirebaseMethods mFirebaseMethods;

    //widgets
    private EditText mCaption;
    private ProgressBar mProgressBar;


    //vars
    private String mAppend = "file:/";
    private int imageCount = 0;
    private String imgUrl;
    private String ImgUrlAppended;
    private Uri imageUri;
    private Bitmap bitmap;
    private Intent intent;
    private byte[] mUploadBytes;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_next);
        mFirebaseMethods = new FirebaseMethods(NextActivity.this);
        mCaption = (EditText) findViewById(R.id.caption) ;
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        setupFirebaseAuth();

      NextActivity.this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        ImageView backArrow = (ImageView) findViewById(R.id.ivBackArrow);
        backArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: closing the activity");
                finish();
            }
        });


        TextView share = (TextView) findViewById(R.id.tvShare);
        share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: navigating to the final share screen.");


                imgUrl = intent.getStringExtra(getString(R.string.selected_image));
                ImgUrlAppended = "file://" + imgUrl;
                Log.d(TAG, "Image URL" + imgUrl);
                    imageUri = Uri.parse(ImgUrlAppended) ;
                Log.d(TAG, "Image URI" + imageUri);

//                //Compress the image
                //ImageCompression
               BackgroundImageResize backgroundImageResize = new BackgroundImageResize(bitmap);
                backgroundImageResize.execute(imageUri);


                //upload the image to firebase
                Toast.makeText(NextActivity.this, "Attempting to upload new photo", Toast.LENGTH_SHORT).show();
                String caption = mCaption.getText().toString();

                if(intent.hasExtra(getString(R.string.selected_image))){
                    imgUrl = intent.getStringExtra(getString(R.string.selected_image));
                    mFirebaseMethods.uploadNewPhoto(getString(R.string.new_photo), caption, imageCount, imgUrl,null);
                }
                else if(intent.hasExtra(getString(R.string.selected_bitmap))){
                    bitmap = (Bitmap) intent.getParcelableExtra(getString(R.string.selected_bitmap));
                    mFirebaseMethods.uploadNewPhoto(getString(R.string.new_photo), caption, imageCount, null,bitmap);
                }



            }
        });

        setImage();
    }


    /**
     * gets the image url from the incoming intent and displays the chosen image
     */
    private void setImage(){
        intent = getIntent();
        ImageView image = (ImageView) findViewById(R.id.imageShare);

        if(intent.hasExtra(getString(R.string.selected_image))){
            imgUrl = intent.getStringExtra(getString(R.string.selected_image));
            Log.d(TAG, "setImage: got new image url: " + imgUrl);
            UniversalImageLoader.setImage(imgUrl, image, null, mAppend);
        }
        else if(intent.hasExtra(getString(R.string.selected_bitmap))){
            bitmap = (Bitmap) intent.getParcelableExtra(getString(R.string.selected_bitmap));
            Log.d(TAG, "setImage: got new bitmap");
            image.setImageBitmap(bitmap);
        }
    }


     /*
       ------------------------------------ Image Compression ---------------------------------------------
     */


    public class BackgroundImageResize extends AsyncTask<Uri, Integer, byte[]>{

        Bitmap mBitmap;

        public BackgroundImageResize(Bitmap bitmap) {
            if(bitmap != null){
                this.mBitmap = bitmap;
            }
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Toast.makeText(NextActivity.this, "compressing image", Toast.LENGTH_SHORT).show();
            //showProgressBar();
        }

        @Override
        protected byte[] doInBackground(Uri... params) {
            Log.d(TAG, "doInBackground: started.");

            Log.d(TAG, "+++++++++++++ params[0]: " + params[0]);

            if(mBitmap == null){
                try{
                    mBitmap = MediaStore.Images.Media.getBitmap(NextActivity.this.getContentResolver(), params[0]);
                }catch (IOException e){
                    Log.e(TAG, "doInBackground: IOException: " + e.getMessage());
                }
            }
            Log.d(TAG, "+++++++++++++ mBitmap: " + mBitmap );


            byte[] bytes = null;
            Log.d(TAG, "doInBackground: megabytes before compression: " + mBitmap.getByteCount() / 1000000 );

            bytes = getBytesFromBitmap(mBitmap, 100);

            Log.d(TAG, "doInBackground: megabytes after compression: " + bytes.length / 1000000 );
            return bytes;
        }

        @Override
        protected void onPostExecute(byte[] bytes) {
            super.onPostExecute(bytes);
            mUploadBytes = bytes;
            //hideProgressBar();
            //execute the upload task


        }
    }


    public static byte[] getBytesFromBitmap(Bitmap bitmap, int quality){
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality,stream);
        return stream.toByteArray();
    }


















     /*
     ------------------------------------ Firebase ---------------------------------------------
     */

    /**
     * Setup the firebase auth object
     */
    private void setupFirebaseAuth(){
        Log.d(TAG, "setupFirebaseAuth: setting up firebase auth.");
        mAuth = FirebaseAuth.getInstance();
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        myRef = mFirebaseDatabase.getReference();
        Log.d(TAG, "onDataChange: image count: " + imageCount);

        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();


                if (user != null) {
                    // User is signed in
                    Log.d(TAG, "onAuthStateChanged:signed_in:" + user.getUid());
                } else {
                    // User is signed out
                    Log.d(TAG, "onAuthStateChanged:signed_out");
                }
                // ...
            }
        };


        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                imageCount = mFirebaseMethods.getImageCount(dataSnapshot);
                Log.d(TAG, "onDataChange: image count: " + imageCount);

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }


    @Override
    public void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }
}
