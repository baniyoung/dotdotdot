package com.example.firebasetest;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.media.ThumbnailUtils;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.firebasetest.ml.Model;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.Date;

import app.akexorcist.bluetotohspp.library.BluetoothSPP;
import app.akexorcist.bluetotohspp.library.BluetoothState;

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private BluetoothSPP bt;//블루투스
    String Address="98:D3:31:F7:9E:0D";//블루투스 모듈 주소

    ImageView imageView;//찍은 사진 화면
    CameraSurfaceView surfaceView;//카메라 화면

    private TextToSpeech myTTS;//음성출력

    TextView result;//결과 텍스트뷰

    int imageSize=224;//이미지 사이즈 정의
    int maxPos;

    String simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
    Bitmap bitmap;//비트맵 정의
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.imageView);
        surfaceView = findViewById(R.id.surfaceView);

        Button button = findViewById(R.id.button);

        result=findViewById(R.id.result);
        myTTS = new TextToSpeech(this, this);

        bt = new BluetoothSPP(this); //Initializing
        if (!bt.isBluetoothAvailable()) { //블루투스 사용 불가
            Toast.makeText(getApplicationContext()
                    , "Bluetooth is not available"
                    , Toast.LENGTH_SHORT).show();
            finish();
        }
        bt.setOnDataReceivedListener(new BluetoothSPP.OnDataReceivedListener() { //데이터 수신
            public void onDataReceived(byte[] data, String message) {
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
        bt.setBluetoothConnectionListener(new BluetoothSPP.BluetoothConnectionListener() { //연결됐을 때
            public void onDeviceConnected(String name, String address) {
                Toast.makeText(getApplicationContext()
                        , "Connected to " + name + "\n" + address
                        , Toast.LENGTH_SHORT).show();
                    String myText = "블루투스 연결되었습니다.";
                    myTTS.speak(myText, TextToSpeech.QUEUE_FLUSH, null);
            }

            public void onDeviceDisconnected() { //연결해제
                Toast.makeText(getApplicationContext()
                        , "Connection lost", Toast.LENGTH_SHORT).show();
            }

            public void onDeviceConnectionFailed() { //연결실패
                Toast.makeText(getApplicationContext()
                        , "Unable to connect", Toast.LENGTH_SHORT).show();
            }
        });

        //촬영 버튼을 눌렀을 때
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //카메라 사진 캡쳐
                capture();
            }

        });

    }

    //카메라 권한 설정
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case 101:
                if(grantResults.length > 0){
                    if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                        Toast.makeText(this, "카메라 권한 사용자가 승인함",Toast.LENGTH_LONG).show();
                    }
                    else if(grantResults[0] == PackageManager.PERMISSION_DENIED){
                        Toast.makeText(this, "카메라 권한 사용자가 허용하지 않음.",Toast.LENGTH_LONG).show();
                    }
                    else{
                        Toast.makeText(this, "수신권한 부여받지 못함.",Toast.LENGTH_LONG).show();
                    }
                }
        }
    }

    //사진 촬영 함수
    public void capture(){
        surfaceView.capture(new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                //bytearray 형식으로 전달
                //이걸이용해서 이미지뷰로 보여주거나 파일로 저장
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 8; // 1/8사이즈로 보여주기
                bitmap = BitmapFactory.decodeByteArray(data, 0, data.length); //data 어레이 안에 있는 데이터 불러와서 비트맵에 저장

                int width = bitmap.getWidth();
                int height = bitmap.getHeight();

                Matrix matrix = new Matrix();

                matrix.postRotate(90);//사진 90도 회전

                Bitmap resizedBitmap = Bitmap.createBitmap(bitmap, 0,0,width,height,matrix,true);

                camera.startPreview();

                int dimension=Math.min(resizedBitmap.getWidth(), resizedBitmap.getHeight());
                resizedBitmap= ThumbnailUtils.extractThumbnail(resizedBitmap, dimension, dimension);
                imageView.setImageBitmap(resizedBitmap);

                resizedBitmap=Bitmap.createScaledBitmap(resizedBitmap, imageSize, imageSize, false);//비트맵 사이즈 조절
                classifyImage(resizedBitmap);//촬영된 이미지 비트맵 분류
                //String myText3 = "촬영되었습니다.";
                //myTTS.speak(myText3, TextToSpeech.QUEUE_FLUSH, null);
                //MediaStore.Images.Media.insertImage(getContentResolver(), resizedBitmap, simpleDateFormat, "");
            }
        });
    }

    //이미지 분류 모델 함수
    public void classifyImage(Bitmap image){
        try {
            Model model = Model.newInstance(getApplicationContext());

            // Creates inputs for reference.
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 224, 224, 3}, DataType.FLOAT32);
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3);
            byteBuffer.order(ByteOrder.nativeOrder());

            int [] intValues = new int[imageSize * imageSize];
            image.getPixels(intValues, 0, image.getWidth(), 0, 0, image.getWidth(), image.getHeight());
            int pixel =0;
            for(int i=0;i<imageSize; i++){
                for(int j=0;j<imageSize;j++){
                    int val = intValues[pixel++];
                    byteBuffer.putFloat(((val>>16)&0xFF)*(1.f/255.f));
                    byteBuffer.putFloat(((val>>8)&0xFF)*(1.f/255.f));
                    byteBuffer.putFloat((val&0xFF)*(1.f/255.f));
                }
            }
            inputFeature0.loadBuffer(byteBuffer);

            // Runs model inference and gets result.
            Model.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

            float[] confidences = outputFeature0.getFloatArray();
            int maxPos = 0;
            float maxConfidence = 0;
            for(int i=0;i<confidences.length;i++){
                if(confidences[i]>maxConfidence){
                    maxConfidence=confidences[i];
                    maxPos=i;
                }
            }
            //학습 음료 배열
            String[] classes = {"이프로", "마운틴듀", "티오피마스터", "사이다", "코카콜라제로", "게토레이", "핫식스", "파워에이드",
                    "스프라이트", "웰치스", "레쓰비", "티오피블랙", "환타", "코카콜라", "맥콜", "갈배", "박카스", "비타오백", "포카리스웨트"};

            result.setText(classes[maxPos]);
            bt.send((String)classes[maxPos], true);
            myTTS.speak((String)classes[maxPos], TextToSpeech.QUEUE_FLUSH, null);


            // Releases model resources if no longer used.
            model.close();
        } catch (IOException e) {
            // TODO Handle the exception
        }

    }

    public void onStart() {
        //앱이 시작하면 블루투스 연결
        super.onStart();
        if (!bt.isBluetoothEnabled()) { //
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            startActivityForResult(intent, BluetoothState.REQUEST_ENABLE_BT);
        } else {
            if (!bt.isServiceAvailable()) {
                bt.setupService();
                bt.startService(BluetoothState.DEVICE_OTHER); //DEVICE_ANDROID는 안드로이드 기기 끼리
            }
        }
    }


    public void onInit ( int status){
        //음성 안내 출력 및 블루투스 자동 연결
        String myText1 = "노란색 버튼은 하단에 있습니다.";
        String myText2 = "노란색 버튼을 누르면 사진이 찍힙니다.";
        myTTS.speak(myText1, TextToSpeech.QUEUE_FLUSH, null);
        myTTS.speak(myText2, TextToSpeech.QUEUE_ADD, null);
        bt.connect(Address);
    }


    @Override
    protected void onDestroy () {
        super.onDestroy();
        myTTS.shutdown();
        bt.stopService(); //블루투스 중지
    }


}