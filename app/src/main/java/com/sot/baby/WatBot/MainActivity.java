package com.sot.baby.WatBot;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import com.ibm.watson.developer_cloud.android.library.audio.MicrophoneInputStream;
import com.ibm.watson.developer_cloud.android.library.audio.StreamPlayer;
import com.ibm.watson.developer_cloud.android.library.audio.utils.ContentType;
import com.ibm.watson.developer_cloud.conversation.v1.ConversationService;
import com.ibm.watson.developer_cloud.conversation.v1.model.MessageRequest;
import com.ibm.watson.developer_cloud.conversation.v1.model.MessageResponse;
import com.ibm.watson.developer_cloud.speech_to_text.v1.SpeechToText;
import com.ibm.watson.developer_cloud.speech_to_text.v1.model.RecognizeOptions;
import com.ibm.watson.developer_cloud.speech_to_text.v1.model.SpeechResults;
import com.ibm.watson.developer_cloud.speech_to_text.v1.websocket.RecognizeCallback;
//import com.ibm.watson.developer_cloud.text_to_speech.v1.TextToSpeech;
import com.ibm.watson.developer_cloud.text_to_speech.v1.model.Voice;
import com.squareup.picasso.Picasso;
import com.wang.avi.AVLoadingIndicatorView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.content.Intent;

//Node.js 서버 연결
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

import static com.ibm.watson.developer_cloud.android.library.camera.CameraHelper.REQUEST_IMAGE_CAPTURE;


public class MainActivity extends AppCompatActivity implements AsyncResponse{


    private static String TAG = "MainActivity";
    private RecyclerView recyclerView;
    private ChatAdapter mAdapter;
    private ArrayList messageArrayList;
    private EditText inputMessage;
    private ImageButton btnSend;
    private ImageButton btnRecord;
    private ImageView imgReocding;
    private Map<String,Object> context = new HashMap<>();
    StreamPlayer streamPlayer;
    private boolean initialRequest;
    private boolean permissionToRecordAccepted = false;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static final int RECORD_REQUEST_CODE = 101;
    private boolean listening = false;
    private SpeechToText speechService;
    private MicrophoneInputStream capture;

    //google API
    private Intent i;
    private SpeechRecognizer  mRecognizer;
    private TextToSpeech tts;

    //VR
    private ResultAdapter resultAdapter;
    private List<Result> output =new ArrayList<>();
    private static final int CAMERA_REQUEST = 1888;
    private static int RESULT_LOAD_IMAGE = 1;
    private ImageView imageView;
    private AVLoadingIndicatorView avi;
    private TextView loadtext;
    private Cursor cursor;
    private  int permissionCheck1, permissionCheck2,permissionCheck3;
    private static final int MY_PERMISSIONS_REQUEST_READ_STORAGE=245;
    private static final int MY_PERMISSIONS_REQUEST_WRITE_STORAGE=121;
    private static final int MY_PERMISSIONS_REQUEST_CAMERA=174;
    private Uri photoURI;
    private File photoFile;
    private String VR_Total;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //VR 사진, 카메라 권한
        permissionCheck1 = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        permissionCheck2 = ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE);
        permissionCheck3 = ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA  );
        resultAdapter= new ResultAdapter(this);
        //VR 이미지 뷰
        this.imageView = (ImageView)this.findViewById(R.id.imageView1);
        avi =(AVLoadingIndicatorView)this.findViewById(R.id.indicator);
        loadtext=(TextView)this.findViewById(R.id.loadtext);
        avi =(AVLoadingIndicatorView)this.findViewById(R.id.indicator);
        stopAnim();

        //VR 권한 체크
        //checkPermisson();


        inputMessage = (EditText) findViewById(R.id.message);
        btnSend = (ImageButton) findViewById(R.id.btn_send);
        btnRecord= (ImageButton) findViewById(R.id.btn_record);
        imgReocding = (ImageView) findViewById(R.id.img_recoding);
        String customFont = "Montserrat-Regular.ttf";
        Typeface typeface = Typeface.createFromAsset(getAssets(), customFont);
        inputMessage.setTypeface(typeface);
        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);

        messageArrayList = new ArrayList<>();
        mAdapter = new ChatAdapter(messageArrayList);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(mAdapter);
        this.inputMessage.setText("");
        this.initialRequest = true;
        sendMessage();



        //google

        i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR");
        i.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
        i.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 30000);



        Bundle extras = getIntent().getExtras();

//        int notyId=0;
//        if (extras == null) {
//        }
//        else {
//            notyId = extras.getInt("notificationId");
//        }

        //google TTS
        tts=new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    tts.setLanguage(Locale.KOREAN);
                }
            }
        });



        //Watson TTS
        final com.ibm.watson.developer_cloud.text_to_speech.v1.TextToSpeech service = new com.ibm.watson.developer_cloud.text_to_speech.v1.TextToSpeech();
        service.setUsernameAndPassword("fc5832bb-316d-413a-8ecb-5df925db188d", "EiF5eQ22MWty");

        int permission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "Permission to record denied");
            makeRequest();
        }


        recyclerView.addOnItemTouchListener(new RecyclerTouchListener(getApplicationContext(), recyclerView, new ClickListener() {
            @Override
            public void onClick(View view, final int position) {
                Thread thread = new Thread(new Runnable() {
                    public void run() {
                        Message audioMessage;
                        try {

                            audioMessage =(Message) messageArrayList.get(position);
                            streamPlayer = new StreamPlayer();
                            if(audioMessage != null && !audioMessage.getMessage().isEmpty())

                                streamPlayer.playStream(service.synthesize(audioMessage.getMessage(), Voice.EN_LISA).execute());
                            else
                                streamPlayer.playStream(service.synthesize("No Text Specified", Voice.EN_LISA).execute());

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
                thread.start();
            }

            @Override
            public void onLongClick(View view, int position) {
                recordMessage();

            }
        }));

        //대화내용 송신
        btnSend.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                if(checkInternetConnection()) {
                    sendMessage();
                }
            }
        });

        //TTS turn On
        btnRecord.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                recordMessage();
            }
        });
    };

    //action Service
    public interface Actioninterface {
        @Headers({"Accept: application/json"})
        @POST("action")
        Call<MessageResponse> actionService(@Body MessageResponse response);

    }
    // Speech to Text Record Audio permission
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case REQUEST_RECORD_AUDIO_PERMISSION:
                permissionToRecordAccepted  = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
            case RECORD_REQUEST_CODE: {

                if (grantResults.length == 0
                        || grantResults[0] !=
                        PackageManager.PERMISSION_GRANTED) {

                    Log.i(TAG, "Permission has been denied by user");
                } else {
                    Log.i(TAG, "Permission has been granted by user");
                }
                return;
            }
        }
        if (!permissionToRecordAccepted ) finish();

    }

    protected void makeRequest() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.RECORD_AUDIO},
                RECORD_REQUEST_CODE);
    }

    // Watson Conversation Service 호출
    private void sendMessage() {

        final String inputmessage = this.inputMessage.getText().toString().trim();
        if(!this.initialRequest) {
            Message inputMessage = new Message();
            inputMessage.setMessage(inputmessage);
            inputMessage.setId("1");
            messageArrayList.add(inputMessage);
        }
        else
        {
            Message inputMessage = new Message();
            inputMessage.setMessage(inputmessage);
            inputMessage.setId("100");
            this.initialRequest = false;
            // Toast.makeText(getApplicationContext(),"Tap on the message for Voice",Toast.LENGTH_LONG).show();

        }

        this.inputMessage.setText("");
        mAdapter.notifyDataSetChanged();

        Thread thread = new Thread(new Runnable(){
            public void run() {
                try {

                    ConversationService service = new ConversationService(ConversationService.VERSION_DATE_2016_09_20);  //버전

                    //SOT용 : service.setUsernameAndPassword("674e6743-7a44-49c0-b27b-0b2cf7774f02", "ei6eHMlhwfa3");
                    service.setUsernameAndPassword("d6c649c3-b984-46ff-8563-a1ac1258ffcf", "FMZa7Fix8fJN");
                    MessageRequest newMessage = new MessageRequest.Builder().inputText(inputmessage).context(context).build();

                    MessageResponse response = service.message("26d08a06-a8f8-4ffe-9b1b-4a86bf1bc3d1", newMessage).execute();

                    //Node.js 서버 활용시
                    if(response.getContext().containsKey("action")){
                        //action  evrnt 가 있을 경우 , node.js 서버 연동

                        Log.v("AiLog  :  ", "action  :  "  +response.getContext().containsKey("action"));

                        //TDDO : 민원과장 서버.....cloud 로 변경 예정
                        Retrofit retrofit = new Retrofit.Builder()
                               // .baseUrl("http://10.0.2.2:3001/")  -- Local Test
                                .baseUrl("http://aidemo.iptime.org:3000/")
                                .addConverterFactory(GsonConverterFactory.create())
                                .build();

                        Actioninterface restService = retrofit.create(Actioninterface.class);
                        Call<MessageResponse> call = restService.actionService(response);
                        //rest API 실행
                        try {
                            response =  call.execute().body();
                        }catch (IOException e){
                            Log.v("AiLog  :  ", "rest api call IOException ." +e);
                        }
                        //응답 body (json) 을 기존 response에 담기


                    }
                    Log.v("AiLog  :  ", "MessageResponse response :  "  +response);
                    if(response.getContext() !=null)
                    {
                        context.clear();
                        context = response.getContext();
                    }
                    Message outMessage=new Message();


                    if(response!=null)
                    {
                        if(response.getOutput()!=null && response.getOutput().containsKey("text"))
                        {
                            //응답 메시지 from Node
                            if(response.getOutput().get("text").toString().startsWith("[")){
                                    String outputText ="";
                                    ArrayList responseList = (ArrayList) response.getOutput().get("text");
                                    if(null !=responseList && responseList.size()>0){
                                         for(int i=0; i<responseList.size();i++){
                                             if(i == 0) {
                                                 outputText = (String)responseList.get(i);
                                            }else{
                                                 outputText += (String)responseList.get(i);
                                            }
                                        }
                                     }
                                outMessage.setMessage(outputText);
                                outMessage.setId("2");
                             //응답 메시지 from watson
                            }else{
                                final String outputmessage = response.getOutput().get("text").toString().replace("[","").replace("]","");
                                outMessage.setMessage(outputmessage);
                                outMessage.setId("2");
                            }
                            messageArrayList.add(outMessage);

                            //Google  TTS Service
//                            if(inputmessage.length() > 0) {
//                                HashMap<String, String> map = new HashMap<>();
//                                map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "MessageId");
//                            //    tts.speak(outputmessage, TextToSpeech.QUEUE_FLUSH, map);
//                            }
                        }

                        runOnUiThread(new Runnable() {
                            public void run() {
                                mAdapter.notifyDataSetChanged();
                                if (mAdapter.getItemCount() > 1) {
                                    recyclerView.getLayoutManager().smoothScrollToPosition(recyclerView, null, mAdapter.getItemCount()-1);
                                }
                            }
                        });

//                        //For 로밍PoC  :   상단바....SMS_AUTH
//                        if(response.getContext().get("action").toString().contains("SMS_AUTH")){
//                            Notification();
//
//                        //PPT OPEN
//                        }else if(response.getContext().get("action").toString().contains("PPT_OPEN")){
//                            openPptPopup();
//                        //쿠폰 발행
//                        }else if(response.getContext().get("action").toString().contains("ISSUED_COUPON")){
//                            openImagePopup();
//                         };
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK ) {
            if(android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                Uri tempUri = data.getData();
                File finalFile = new File(getPath(tempUri));
                Picasso.with(this)
                        .load(finalFile)
                        .placeholder(R.drawable.ph1)
                        .into(imageView);
                startAnim();
                FetchTask search = new FetchTask(finalFile,this);
                search.execute();

            }
            else {
                Picasso.with(this)
                        .load(photoFile)
                        .placeholder(R.drawable.ph1)
                        .into(imageView);
                startAnim();
                FetchTask search = new FetchTask(photoFile, this);
                search.execute();
            }

        }
        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data) {
            Uri selectedImage = data.getData();
            String[] filePathColumn = { MediaStore.Images.Media.DATA };

            cursor = getContentResolver().query(selectedImage,
                    filePathColumn, null, null, null);
            cursor.moveToFirst();
            Uri tempUri = data.getData();

            File finalFile = new File(getPath(tempUri));

            Picasso.with(this)
                    .load(finalFile)
                    .placeholder(R.drawable.ph1)
                    .into(imageView);
            startAnim();
            FetchTask search = new FetchTask(finalFile,this);
            search.execute();

        }
    }
    public String getPath(Uri uri) {
        if( uri == null ) {
            return null;
        }
        String[] projection = { MediaStore.Images.Media.DATA };
        cursor = managedQuery(uri, projection, null, null, null);
        if( cursor != null ){
            int column_index = cursor
                    .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            String path = cursor.getString(column_index);
            return path;
        }
        return uri.getPath();
    }
    //상단 오른쪽 카메라와 사진 폴더
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.acitivity_main_menu, menu);

        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {


            if (item.getItemId() == R.id.action_camera) {
                openActionCarmera();
             }
            if(item.getItemId()==R.id.action_browse){
                openActionBrowse();

            }
        return super.onOptionsItemSelected(item);
    }
    //VR 카메라 OPEN
    public void openActionCarmera() {
        Log.v("AiLog  :  ", "action_camera 1" + permissionCheck1);
        Log.v("AiLog  :  ", "action_camera 2" + permissionCheck2);
        Log.v("AiLog  :  ", "action_camera 3" + permissionCheck3);
        if(permissionCheck1==PackageManager.PERMISSION_GRANTED &&
                permissionCheck2==PackageManager.PERMISSION_GRANTED &&
                permissionCheck3==PackageManager.PERMISSION_GRANTED) {

            Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
            if(android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                startActivityForResult(cameraIntent, REQUEST_IMAGE_CAPTURE);
            }else{
            if (cameraIntent.resolveActivity(getPackageManager()) != null) {
                // Create the File where the photo should go
                photoFile = null;
                try {
                    photoFile = createImageFile();
                } catch (IOException ex) {
                    // Error occurred while creating the File
                }
                // Continue only if the File was successfully created
                if (photoFile != null) {
                    photoURI = FileProvider.getUriForFile(this,
                            "com.sot.baby.Watbot.fileprovider",
                            photoFile);
                    cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                    startActivityForResult(cameraIntent, REQUEST_IMAGE_CAPTURE);
                }
            }
        }}
        else{
            checkPermisson();

        }

    }

    //VR 사진 브라우저 OPEN
    public void openActionBrowse() {
        Log.v("AiLog  :  ", "action_browse 1" + permissionCheck1);
        Log.v("AiLog  :  ", "action_browse 2" + permissionCheck2);
        if(permissionCheck1==PackageManager.PERMISSION_GRANTED && permissionCheck2==PackageManager.PERMISSION_GRANTED) {
            Intent i = new Intent(
                    Intent.ACTION_PICK,
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

            startActivityForResult(i, RESULT_LOAD_IMAGE);

        }
        else{
            checkPermisson();
        }
    }


    String mCurrentPhotoPath;
    //VR
    private File createImageFile() throws IOException {
        // Create an image file name

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    //VR 사진 응답
    @Override
    public void processFinish(List<Result> output) {
        this.output=output;
        setResultAdapter();


    }

    private void setResultAdapter(){

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                resultAdapter.setResultList(output);
                WatsonVRTask task = new WatsonVRTask();
                task.execute();
                stopAnim();
            }
        });
    }

    private class WatsonVRTask extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... textToSpeak) {

            //  TextToSpeech textToSpeech = initTextToSpeechService();
            streamPlayer = new StreamPlayer();
            List<String> results = new ArrayList<>();

            String oName = null;
            Double oScore = 0.0;
            Log.v("AiLog  :  ", "WatsonVRTask Vtput.size() :  "  + output.size());
            if(output.size()>0){
                for(int i=0;i<output.size();i++) {
                    if(i == 0) {
                        oName = output.get(0).getname();
                        oScore = output.get(0).getscore();
                        VR_Total = i + " : " + oName + " ("+ oScore + ")";
                    }else{
                        VR_Total += "\n" + i + " : " + output.get(i).getname() + " ("+ output.get(i).getscore() + ")";
                    }
                    if(oScore < output.get(i).getscore() ) {
                        oName = output.get(i).getname();
                        oScore = output.get(i).getscore();
                    }


                    results.add(output.get(i).getname());
                }}
            VR_Total += "\n\n" +"최고득점 : " +oName + " ("+oScore+")";

            Log.v("AiLog  :  ", "oName :  "  + oName + " , oScore : " + oScore);
            Log.v("AiLog  :  ", "VR_Total :  "  + VR_Total);
            return null;
        }
    }


    //이미지 팝업
    public void openImagePopup(){
        //데이터 담아서 팝업(액티비티) 호출
        Intent intent = new Intent(this, PopupActivity.class);
        intent.putExtra("data", "Test Popup");

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        startActivityForResult(intent, 1);
    }
    //이미지 팝업
    public void openPptPopup(){
        //데이터 담아서 팝업(액티비티) 호출
        Intent intent = new Intent(this, PowerpointActivity.class);
        intent.putExtra("data", "Test Popup");
        startActivityForResult(intent, 1);
    }
    //상단바 알림
    public void Notification() {

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Resources res = getResources();

      Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.putExtra("notificationId", 234563); //전달할 값
       PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

        builder.setContentTitle("SKT 로밍센터")
                .setContentText("인증번호는 [234563] 입니다.")
                .setTicker("인증번호를 입력하세요")
                .setSmallIcon(R.mipmap.ic_sk)
                .setLargeIcon(BitmapFactory.decodeResource(res, R.mipmap.ic_sk))
      //          .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setWhen(System.currentTimeMillis())
                .setDefaults(android.app.Notification.DEFAULT_ALL);



        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            builder.setCategory(android.app.Notification.CATEGORY_MESSAGE)
                    .setPriority(android.app.Notification.PRIORITY_HIGH)
                    .setVisibility(android.app.Notification.VISIBILITY_PUBLIC);
        }
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(1234, builder.build());
    }

    //Record a message via Watson Speech to Text
    private void recordMessage() {
        //mic.setEnabled(false);

        /** watson 주석
         speechService = new SpeechToText();
         speechService.setUsernameAndPassword("b8a015ee-5f7e-40a4-a8f7-0e76bfb63017", "lbaZnyVbFBNQ");

         if(listening != true) {
         capture = new MicrophoneInputStream(true);
         new Thread(new Runnable() {
        @Override public void run() {
        try {
        speechService.recognizeUsingWebSocket(capture, getRecognizeOptions(), new MicrophoneRecognizeDelegate());
        } catch (Exception e) {
        showError(e);
        }
        }
        }).start();
         listening = true;
         Toast.makeText(MainActivity.this,"Listening....Click to Stop", Toast.LENGTH_LONG).show();

         } else {
         try {
         capture.close();
         listening = false;
         Toast.makeText(MainActivity.this,"Stopped Listening....Click to Start", Toast.LENGTH_LONG).show();
         } catch (Exception e) {
         e.printStackTrace();
         }

         }
         */

        //google
        Log.v("AiLog  :  ", "recordMessage :  "  + "recode start.");

        mRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        mRecognizer.setRecognitionListener(listener);
        mRecognizer.startListening(i);


    }

    /**
     * Check Internet Connection
     * @return
     */
    private boolean checkInternetConnection() {
        // get Connectivity Manager object to check connection
        ConnectivityManager cm =
                (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();

        // Check for network connections
        if (isConnected){
            return true;
        }
        else {
            Toast.makeText(this, " No Internet Connection available ", Toast.LENGTH_LONG).show();
            return false;
        }

    }

    //Private Methods - Speech to Text
    private RecognizeOptions getRecognizeOptions() {
        return new RecognizeOptions.Builder()
                .continuous(true)
                .contentType(ContentType.OPUS.toString())
                //.model("en-UK_NarrowbandModel")
                .interimResults(true)
                .inactivityTimeout(2000)
                .build();
    }

    private class MicrophoneRecognizeDelegate implements RecognizeCallback {

        @Override
        public void onTranscription(SpeechResults speechResults) {
            System.out.println(speechResults);
            if(speechResults.getResults() != null && !speechResults.getResults().isEmpty()) {
                String text = speechResults.getResults().get(0).getAlternatives().get(0).getTranscript();
                showMicText(text);
            }
        }
        @Override public void onConnected() {
        }
        @Override public void onError(Exception e) {
            showError(e);
            enableMicButton();
        }
        @Override public void onDisconnected() {
            enableMicButton();
        }
    }
    private void showMicText(final String text) {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                inputMessage.setText(text);
            }
        });
    }
    private void enableMicButton() {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                btnRecord.setEnabled(true);
            }
        });
    }
    private void showError(final Exception e) {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        });
    }
    //google
    private RecognitionListener listener = new RecognitionListener() {
        @Override
        public void onRmsChanged(float rmsdB) {
            // TODO Auto-generated method stub
            // Log.v("AiLog  :  ", "onRmsChanged :  "  + rmsdB);
        }
        @Override
        public void onResults(Bundle results) {
            Log.v("AiLog  :  ", "onResults key:  "  + results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION));
            String key = "";
            key = SpeechRecognizer.RESULTS_RECOGNITION;


            ArrayList<String> mResult = results.getStringArrayList(key);

            String[] rs = new String[mResult.size()];
            Log.v("AiLog  :  ", "rs.length:  "  + rs.length);
            System.out.println("rs : " + rs);
            mResult.toArray(rs);

            Log.v("AiLog  :  ", "mResult.size():  "  + mResult.size());
            //tv.setText(""+rs[0]);
            inputMessage.setText(""+rs[0]);

            if(inputMessage.getText().toString().replace(" ","").contentEquals("사진올릴래"))
            {
                //VR 사진 올리기
                Log.v("AiLog  :  ", "mResult.getT1ext:  "  + inputMessage.getText().toString().replace(" ",""));
                openActionBrowse();
            }

        }
        @Override
        public void onReadyForSpeech(Bundle params) {
            // TODO Auto-generated method stub
            Log.v("AiLog  :  ", "onReadyForSpeech :  "  + params.toString());
        }
        @Override public void onPartialResults(Bundle partialResults) {
            // TODO Auto-generated method stub
            Log.v("AiLog  :  ", "onPartialResults :  "  + partialResults.toString());
        }
        @Override public void onEvent(int eventType, Bundle params) {
            // TODO Auto-generated method stub
            Log.v("AiLog  :  ", "onEvent :  "  + params.toString());

        }
        @Override public void onError(int error) {
            // TODO Auto-generated method stub
        }
        @Override public void onEndOfSpeech() {
            // TODO Auto-generated method stub
            Log.v("AiLog  :  ", "onEndOfSpeech :  "  + "onEndOfSpeech");
            imgReocding.setVisibility(View.INVISIBLE);
        }
        @Override public void onBufferReceived(byte[] buffer) {
            // TODO Auto-generated method stub
            Log.v("AiLog  :  ", "onBufferReceived :  "  + buffer.length) ;
        }
        @Override public void onBeginningOfSpeech() {
            // TODO Auto-generated method stub
            Log.v("AiLog  :  ", "onBeginningOfSpeech :  "  + "onBeginningOfSpeech") ;
            imgReocding.setVisibility(View.VISIBLE);
        }
    };

    //VR 사진 , 카메라 권한 체크
    public void checkPermisson(){
        if(permissionCheck1!=PackageManager.PERMISSION_GRANTED ) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},MY_PERMISSIONS_REQUEST_READ_STORAGE);
        }
        if( permissionCheck2!=PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},MY_PERMISSIONS_REQUEST_WRITE_STORAGE);
        }
        if( permissionCheck3!=PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},MY_PERMISSIONS_REQUEST_CAMERA);
        }

    }

    void startAnim(){

      //  VR_Total = null;
        avi.show();
        loadtext.setVisibility(View.VISIBLE);
    }
    void stopAnim(){
        avi.hide();
        loadtext.setVisibility(View.GONE);

        Log.v("AiLog  :  ", "VR_Total :  "  + VR_Total) ;
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if(VR_Total !=  null) {
           // Toast.makeText(this, VR_Total, Toast.LENGTH_LONG).show();

            Message oututMessage = new Message();
            oututMessage.setMessage(VR_Total);
            oututMessage.setId("2");
            messageArrayList.add(oututMessage);;

        }
    }

    @Override
    protected void onDestroy() {

        if(cursor!=null){
            cursor.close();}

        if(tts !=null){
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();


    }

}

