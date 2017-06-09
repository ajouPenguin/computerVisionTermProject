package com.example.administrator.computervision;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.Buffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.jar.Manifest;

import static android.R.attr.category;
import static android.R.attr.data;
import static android.R.attr.queryActionMsg;
import static android.R.attr.yearListSelectorColor;


public class MainActivity extends AppCompatActivity {

    Button btn_takePicture, btn_getAlbum, send;
    ImageView iv_capture;
    ImageView resize;
    Bitmap bitmap;
    Bitmap resized;
    TextView resultTxt;
    int REQUEST_CAPTURE = 1;
    int REQUEST_ALBUM = 2;
    byte[] bitmap_bytes;
    private String imgPath = "";
    String line = "";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        iv_capture = (ImageView) findViewById(R.id.iv);   //선택하거나 찍은 이미지를 보여준다.
        btn_takePicture = (Button) findViewById(R.id.cbtn);  // 카메라 아이콘 버튼
        btn_getAlbum = (Button)findViewById(R.id.gbtn); // 갤러리 아이콘 버튼
        send = (Button)findViewById(R.id.send);
        resultTxt = (TextView)findViewById(R.id.result);

        btn_takePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { // 카메라 버튼을 눌렀을 때  카메라 어플을 킨다.
                Intent itt = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                itt.putExtra(MediaStore.EXTRA_OUTPUT, getFileUri()); //파일의 경로를 요청
                startActivityForResult(itt, REQUEST_CAPTURE);
            }
        });

        btn_getAlbum.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(Intent.createChooser(intent,"Select Picture"),REQUEST_ALBUM);//갤러리 요청
            }
        });

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //resized = Bitmap.createScaledBitmap(bitmap, 300 , 300 , true);
                bitmap_bytes = bitmapToByteArray(bitmap);
                // 서버에 연결하는 코드가 들어간다.
               // resize = (ImageView)findViewById(R.id.resize);
                //resize.setImageBitmap(resized);
                NetworkTask networkTask = new NetworkTask();
                networkTask.execute();
            }
        });
    }
    private Uri getFileUri(){
        File dir = new File(getFilesDir(),"img");
        if(!dir.exists()){
            dir.mkdirs();
        }
        File file = new File(dir, System.currentTimeMillis()+".png");
        imgPath = file.getAbsolutePath();
        return FileProvider.getUriForFile(this, getApplicationContext().getPackageName()+".fileprovider",file);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        if(resultCode == RESULT_OK){
            switch(requestCode){
                case 1:
                    bitmap = BitmapFactory.decodeFile(imgPath);
                    int nh = (int)(bitmap.getHeight()*(1024.0 / bitmap.getWidth()));
                    Bitmap scaled = Bitmap.createScaledBitmap(bitmap, 1024, nh, true);
                    bitmap = rotate(scaled,90);
                    iv_capture.setImageBitmap(bitmap);
                    break;
                case 2:
                    Uri uri = data.getData();
                    try {
                        bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                        nh = (int)(bitmap.getHeight()*(1024.0 / bitmap.getWidth()));
                        scaled = Bitmap.createScaledBitmap(bitmap, 1024, nh, true);
                        bitmap = rotate(scaled, 90);
                        iv_capture.setImageBitmap(bitmap);
                        break;
                    }
                    catch (IOException e){
                        e.printStackTrace();
                    }

            }
        }
        else{
            Toast.makeText(this,"취소 되었습니다. ", Toast.LENGTH_LONG).show();
        }
    }
    public Bitmap rotate(Bitmap bitmap, int degrees){// 90도 rotate
        if(degrees != 0 && bitmap != null){
            Matrix m = new Matrix();
            m.setRotate(degrees);
            try{
                Bitmap converted = Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight(),m,true);
                if(bitmap != converted){
                    bitmap = null;
                    bitmap = converted;
                    converted = null;
                }
            }catch (OutOfMemoryError ex)
            {
                Toast.makeText(getApplicationContext(),"메모리 부족",Toast.LENGTH_SHORT).show();
            }

        }
        return bitmap;
    }
    public byte[] bitmapToByteArray(Bitmap bitmap){  // 서버에 보내기위해  비트맵을  바이트 배열로 변환
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG ,100, stream);
        byte[] byteArray = stream.toByteArray();
        return byteArray;
    }


    public class NetworkTask extends AsyncTask<Void, Void, Void> {
        ProgressDialog asyncDialog = new ProgressDialog(MainActivity.this);
        @Override
        protected void onPreExecute(){
            asyncDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            asyncDialog.setMessage("확인중입니다....");
            asyncDialog.show();
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... arg0) {

            try {
                    //IP주소와 포트번호를 입력하여 Socket 통신을 시작합니다.
                    Socket sock = new Socket("121.169.44.190", 50000);
                    //Socket으로부터 outputStream을 얻습니다.
                    DataOutputStream os = new DataOutputStream(sock.getOutputStream());
                    InputStream is = sock.getInputStream();
                    //등록한 OutputStream을 ObjectOutputStream 방식으로 사용합니다.
                ObjectOutputStream oos = new ObjectOutputStream(os);     //byte[] 파일을 object 방식으로 통째로 전송합니다.
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));

                System.out.println(bitmap_bytes.length);
                os.writeUTF(""+bitmap_bytes.length);
                oos.writeObject(bitmap_bytes);


                line = reader.readLine();
                int byteLength = Integer.parseInt(reader.readLine());
                     System.out.println(line + " "+ byteLength);
                byte[] data = null;
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

                byte[] buf = new byte[1000000];
                int readBytes = 0;
                int sum = 0;

                while (true){//() { //보낸것을 딱맞게 받아 write 합니다.
                    sum += (readBytes = is.read(buf));
                    outputStream.write(buf, 0, readBytes);
                    if(sum >= byteLength)break;
                }
                data = outputStream.toByteArray();
                bitmap = BitmapFactory.decodeByteArray(data, 0 , data.length);

                outputStream.close();
                oos.close();
                os.close();
                reader.close();
                is.close();
                sock.close();
            } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            asyncDialog.dismiss();
            String[] parsing = line.split(" ");
            for(int i=0; i<parsing.length; i++)
                System.out.println(parsing[i]);
            int[] k = new int[parsing.length];

            for(int i=0; i<parsing.length; i++){
                for(int j=i; j<parsing.length; j++){
                    if(parsing[i].equals(parsing[j])){
                        k[i]++;
                    }
                }
            }
            String res = "";
            for(int i=0; i<k.length; i++){
                if(k[i]==1){
                    res += parsing[i]+" ";
                }
            }
            resultTxt.setText(res);
            resultTxt.setTextSize(30);
            iv_capture.setImageBitmap(bitmap);

            super.onPostExecute(result);
        }

    }

}


