package com.dreamwalker.uploadphp101;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
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
    Button btnCamera;


    Uri uri;

    ProgressDialog progressDialog;
    File compressedImage;
    File tmpFile;
    File path;

    private IUploadAPI getAPIUpload() {

        return RetrofitClient.getClient(BASE_URL).create(IUploadAPI.class);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // TODO: 2018-05-17 permission

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE,
            }, REQUEST_PERMISSION);
        }


        // TODO: 2018-05-17 service
        mService = getAPIUpload();

        btnUpload = (Button) findViewById(R.id.btn_upload);
        imageView = (ImageView) findViewById(R.id.image_view);
        btnCamera = (Button) findViewById(R.id.btn_camera);

        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseFile();
            }
        });

        btnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (uri != null) {
                    uploadFile();
                } else {
                    Snackbar.make(v, "사진을 업로드하세요", Snackbar.LENGTH_SHORT).show();
                }

            }
        });

        btnCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, Main2Activity.class));
            }
        });

        folderCreate();


    }

    private void uploadFile() {
        if (uri != null) {
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

        switch (requestCode) {
            case REQUEST_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Permission denine", Toast.LENGTH_SHORT).show();
                }
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == PICK_FILE_REQUEST) {
                if (data != null) {
                    uri = data.getData();
                    //path = FileUtils.getPath(this, uri);
                    path = FileUtils.getFile(this, uri);

                    tmpFile = FileUtils.getFile(this, uri);
                    Log.e(TAG, "onActivityResult: path " + path);
                    Log.e(TAG, "onActivityResult: uri " + uri);
                    Log.e(TAG, "onActivityResult: getPath " + uri.getPath());
                    Log.e(TAG, "onActivityResult: getData " + data.getData());
                    if (uri != null && !uri.getPath().isEmpty()) {
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
                                        Log.e(TAG, "accept:  called");
                                    }
                                }, new Consumer<Throwable>() {
                                    @Override
                                    public void accept(Throwable throwable) {
                                        throwable.printStackTrace();
                                        //showError(throwable.getMessage());
                                    }
                                });


                        Log.e(TAG, "onActivityResult: getData " + getReadableFileSize(path.length()));

                    } else {
                        Toast.makeText(this, "Cannot upload file to server ", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }

    private void setCompressedImage() {

        imageView.setImageBitmap(BitmapFactory.decodeFile(compressedImage.getAbsolutePath()));
        Log.e(TAG, "setCompressedImage: compressedImage.getAbsolutePath() " + compressedImage.getAbsolutePath());
        Log.e(TAG, "setCompressedImage: " + String.format("Size : %s", getReadableFileSize(compressedImage.length())));
        //compressedSizeTextView.setText(String.format("Size : %s", getReadableFileSize(compressedImage.length())));
        Log.e(TAG, "setCompressedImage: " + compressedImage.toString());
        Log.e(TAG, "setCompressedImage:  compressedImage.getName(); " + compressedImage.getName());

        Toast.makeText(this, "Compressed image save in " + compressedImage.getPath(), Toast.LENGTH_LONG).show();
        Log.e("Compressor", "Compressed image save in " + compressedImage.getPath());

        fileCopy();

    }

    private void folderCreate() {

        File externalPath = Environment.getExternalStorageDirectory();
        String folderPath = "/Android/data/com.dreamwalker.uploadphp101/files/";
        File mkdirFile = new File(externalPath, folderPath);

        if (!mkdirFile.exists()) {
            mkdirFile.mkdirs();
        } else {
            Log.e(TAG, "folderCreate: " + "folder already existes");
        }
    }

    private void fileCopy() {
        //Context ctx = this; // for Activity, or Service. Otherwise simply get the context.
        //String dbname = "mydb.db";
        // dbpath = ctx.getDatabasePath(dbname);
        Log.e(TAG, "fileCopy: called 1 ");
        File externalPath = Environment.getExternalStorageDirectory();
        String folderPath = "/Android/data/com.dreamwalker.uploadphp101/files/";
        String backupDBPath = "/Android/data/com.dreamwalker.uploadphp101/files/" + compressedImage.getName();

        File mkdirFile = new File(externalPath, folderPath);
        File currentDB = new File(compressedImage.getPath());
        File backupDB = new File(externalPath, backupDBPath);

        if (!mkdirFile.exists()) {
            mkdirFile.mkdirs();
        }
        try {
            if (externalPath.canWrite()) {
                Log.e(TAG, "fileCopy: called  2");

                if (currentDB.exists()) {
                    FileChannel src = new FileInputStream(currentDB).getChannel();
                    FileChannel dst = new FileOutputStream(backupDB).getChannel();
                    dst.transferFrom(src, 0, src.size());
                    src.close();
                    dst.close();
                }
                if (backupDB.exists()) {
                    Log.e(TAG, "fileCopy: File Copy Complete!!");
                    //Toast.makeText(this, "File Copy Complete!!", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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


    private void clearApplicationCacheV2() {
        File dir = getCacheDir();
        if (dir == null) return;

        File[] children = dir.listFiles();
        try {
            // 쿠키 삭제
            //CookieManager cookieManager = CookieManager.getInstance();
            //cookieManager.removeSessionCookie();

            for (int i = 0; i < children.length; i++) {
                Log.e(TAG, "clearApplicationCacheV2: " + children[i].getPath());
                boolean result = children[i].delete();

            }


//            for (File f : children) {
//
//                Log.e(TAG, "clearApplicationCacheV2: " + f.getPath());
//                f.delete();
////                f(children[i].isDirectory()) {
////                    clearApplicationCaches(children[i]);
////                } else {
////                    children[i].delete();
////                }
//            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Toast.makeText(this, "캐쉬 비우기 완료", Toast.LENGTH_SHORT).show();
    }

    /**
     * 앱 캐시 지우기
     * @param context
     */
    public static void clearApplicationData(Context context) {
        File cache = context.getCacheDir();
        File appDir = new File(cache.getParent());
        if (appDir.exists()) {
            String[] children = appDir.list();
            for (String s : children) {
                //다운로드 파일은 지우지 않도록 설정
                if(s.equals("lib") || s.equals("files") || s.equals("code_cache") || s.equals("shaders")) continue;
                deleteDir(new File(appDir, s));
                Log.e("test", "File /data/data/"+context.getPackageName()+"/" + s + " DELETED");
            }
        }
    }

    private static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        return dir.delete();
    }

//    private void clearApplicationCache() {
//        File dir = getCacheDir();
//        if (dir == null) return;
//
//        File[] children = dir.listFiles();
//        try {
//            // 쿠키 삭제
//            //CookieManager cookieManager = CookieManager.getInstance();
//            //cookieManager.removeSessionCookie();
//
//            for (int i = 0; i < children.length; i++) {
//                if (children[i].isDirectory()) {
//                    clearApplicationCaches(children[i]);
//                } else {
//                    children[i].delete();
//                }
//            }
//        } catch (Exception e) {
//        }
//    }
//
//
//    private void clearApplicationCaches(File dir) {
//        if (dir == null) dir = getCacheDir();
//        if (dir == null) return;
//
//        File[] children = dir.listFiles();
//        try {
//            for (int i = 0; i < children.length; i++)
//                if (children[i].isDirectory()) {
//                    clearApplicationCaches(children[i]);
//                } else {
//                    children[i].delete();
//                }
//        } catch (Exception e) {
//        }
//    }

    private void getCachesSize() {
        long size = 0;
        File cacheDirectory = getCacheDir();
        File[] files = cacheDirectory.listFiles();
        for (File f : files) {
            size = size + f.length();
        }
        Toast.makeText(this, "캐쉬 사이즈는 " + getReadableFileSize(size), Toast.LENGTH_SHORT).show();
        Log.e(TAG, "getCachesSize: " + getReadableFileSize(size));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_preview, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.menu_save:
                //clearApplicationCacheV2();
                clearApplicationData(this);
                return true;
            case R.id.menu_check:
                getCachesSize();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }
}
