package com.dreamwalker.uploadphp101;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.dreamwalker.uploadphp101.Remote.IUploadAPI;
import com.dreamwalker.uploadphp101.Remote.RetrofitClient;
import com.dreamwalker.uploadphp101.Utils.ProgressRequestBody;
import com.dreamwalker.uploadphp101.Utils.UploadCallBacks;
import com.ipaulpro.afilechooser.utils.FileUtils;

import java.io.File;
import java.text.DecimalFormat;

import id.zelory.compressor.Compressor;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements UploadCallBacks {
    private static final String TAG = "MainActivity";

    private static final String BASE_URL = "http://kangwonelec.com/";
    private static final int REQUEST_PERMISSION = 1000;
    private static final int PICK_FILE_REQUEST = 1001;

    IUploadAPI mService;

    ImageView imageView;
    Button btnUpload;


    Uri uri;

    ProgressDialog progressDialog;
    File compressedImage;

    private IUploadAPI getAPIUpload(){

        return RetrofitClient.getClient(BASE_URL).create(IUploadAPI.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // TODO: 2018-05-17 permission

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
            }, REQUEST_PERMISSION);
        }


        // TODO: 2018-05-17 service
        mService  = getAPIUpload();

        btnUpload = (Button)findViewById(R.id.btn_upload);
        imageView  = (ImageView)findViewById(R.id.image_view);

        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseFile();
            }
        });

        btnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (uri != null){
                    uploadFile();
                }else {
                    Snackbar.make(v,"사진을 업로드하세요", Snackbar.LENGTH_SHORT).show();
                }

            }
        });


    }

    private void uploadFile() {
        if (uri != null){
            progressDialog = new ProgressDialog(this);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.setMessage("Uploading");
            progressDialog.setIndeterminate(false);
            progressDialog.setMax(100);
            progressDialog.setCancelable(false);
            progressDialog.show();

//            File file = FileUtils.getFile(this, uri);
//            ProgressRequestBody requestBody = new ProgressRequestBody(file, this);
//            final MultipartBody.Part body = MultipartBody.Part.createFormData("uploaded_file", file.getName(), requestBody);

            //File file = FileUtils.getFile(this, uri);
            ProgressRequestBody requestBody = new ProgressRequestBody(compressedImage, this);
            final MultipartBody.Part body = MultipartBody.Part.createFormData("uploaded_file", compressedImage.getName(), requestBody);

            new Thread(new Runnable() {
                @Override
                public void run() {
                        mService.uploadFile(body).enqueue(new Callback<String>() {
                            @Override
                            public void onResponse(Call<String> call, Response<String> response) {
                                progressDialog.dismiss();
                                Toast.makeText(MainActivity.this, "Uploaded", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onFailure(Call<String> call, Throwable t) {
                                Toast.makeText(MainActivity.this, t.getMessage(), Toast.LENGTH_SHORT).show();

                            }
                        });
                }
            }).start();
        }

    }

    private void chooseFile() {

        Intent getContentIntent = Intent.createChooser(FileUtils.createGetContentIntent(), "select file");
        startActivityForResult(getContentIntent, PICK_FILE_REQUEST);


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode){
            case REQUEST_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show();
                }else {
                    Toast.makeText(this, "Permission denine", Toast.LENGTH_SHORT).show();
                }
        }
    }
    File path;
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK){
            if (requestCode == PICK_FILE_REQUEST){
                if (data != null){
                    uri = data.getData();
                    //path = FileUtils.getPath(this, uri);
                    path = FileUtils.getFile(this,uri);
                    Log.e(TAG, "onActivityResult: path " + path );
                    Log.e(TAG, "onActivityResult: uri " + uri );
                    Log.e(TAG, "onActivityResult: getPath " + uri.getPath());
                    Log.e(TAG, "onActivityResult: getData " + data.getData() );
                    if (uri != null && !uri.getPath().isEmpty()){
                        //imageView.setImageURI(uri);

                        new Compressor(this)
                                .compressToFileAsFlowable(path)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(new Consumer<File>() {
                                    @Override
                                    public void accept(File file) {
                                        compressedImage = file;
                                        setCompressedImage();
                                    }
                                }, new Consumer<Throwable>() {
                                    @Override
                                    public void accept(Throwable throwable) {
                                        throwable.printStackTrace();
                                        //showError(throwable.getMessage());
                                    }
                                });
                        Log.e(TAG, "onActivityResult: getData " + getReadableFileSize(path.length()));

                    }else {
                        Toast.makeText(this, "Cannot upload file to server ", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }

    private void setCompressedImage() {
        imageView.setImageBitmap(BitmapFactory.decodeFile(compressedImage.getAbsolutePath()));
        Log.e(TAG, "setCompressedImage: " + String.format("Size : %s", getReadableFileSize(compressedImage.length())) );
       //compressedSizeTextView.setText(String.format("Size : %s", getReadableFileSize(compressedImage.length())));

        Toast.makeText(this, "Compressed image save in " + compressedImage.getPath(), Toast.LENGTH_LONG).show();
        Log.d("Compressor", "Compressed image save in " + compressedImage.getPath());
    }

    @Override
    public void onProgressUpdate(int percentage) {

        progressDialog.setProgress(percentage);

    }

    public String getReadableFileSize(long size) {
        if (size <= 0) {
            return "0";
        }
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }
}
