package com.sot.baby.WatBot;

import android.os.AsyncTask;
import android.util.Log;

import com.ibm.watson.developer_cloud.visual_recognition.v3.VisualRecognition;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.ClassifyImagesOptions;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.VisualClassification;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.VisualClassifier;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class FetchTask extends AsyncTask <Void,Void,Void> {

    private AsyncResponse asyncResponse;
    private File file;
    private List<Result> search_result = new ArrayList<>();

    public FetchTask(File file, AsyncResponse asyncResponse) {
        this.file = file;
        this.asyncResponse=asyncResponse;
    }

    @Override
    protected Void doInBackground(Void... params) {


        VisualRecognition service = new VisualRecognition(VisualRecognition.VERSION_DATE_2016_05_20);

        service.setApiKey("e11dfbd8f2f139186765f1a53a06eaad7d4d4f2b");  //VR KEY




        ClassifyImagesOptions options = new ClassifyImagesOptions.Builder()
                .classifierIds("Baby_Test_90655732")
                .images(file)
                .threshold(0.0)
                .build();

        VisualClassification result = service.classify(options).execute();
        Log.v("AiLog  :  ", "VisualClassification result: " + result);

        if (result.getImages() != null) {

            List<VisualClassifier> resultClasses = result.getImages().get(0).getClassifiers();
            if (resultClasses.size() > 0) {
                VisualClassifier classifier = resultClasses.get(0);
                List<VisualClassifier.VisualClass> classList = classifier.getClasses();
                Log.v("AiLog  :  ", "classList.size()  : " + classList.size()  );
                if (classList.size() > 0) {

                    for(int i=0;i<classList.size();i++) {
                        Log.v("AiLog  :  ", "classifier name : " + classList.get(i).getName() + "  , classifier Score: " +classList.get(i).getScore());

                        Result r =new Result(classList.get(i).getName(),classList.get(i).getScore());
                        search_result.add(r);

                    }
                    if(search_result!=null){

                        asyncResponse.processFinish(search_result);
                    }}}
        }else{
            Log.v("AiLog  :  ", "getClassifiers name : " + result.getImages().get(0).getClassifiers() );
        }
        return null;
    }
}





