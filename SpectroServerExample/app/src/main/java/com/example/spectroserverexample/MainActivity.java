package com.example.spectroserverexample;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;

import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class MainActivity extends AppCompatActivity {

    public static final int STATUS_CODE = 0;
    private Client receiverThread=null;
    private String imageselected = "android-studio_CAT.jpg";
    private int countimage=0;

    private List<String> displayedImage = new ArrayList<String>();

    private static Context context; //application context
    private Handler mainThreadHandler;
    private Toast toast;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        displayedImage.add("android-studio_CAT.jpg");
        displayedImage.add("refpect-android.jpg");
        displayedImage.add("grumpycat.jpg");
        displayedImage.add("shiba-inu.jpg");

        super.onCreate(savedInstanceState);
        context = this;
        setContentView(R.layout.activity_main);
        //Set Up Button
        Button click = (Button)findViewById(R.id.button);

        //Sets Up OnClick Listener For Button
        click.setOnClickListener(new View.OnClickListener() {


            @Override
            public void onClick(View v) {
                //Activly Requests Acsess To Files (With Pop-Up)

                if (ContextCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            STATUS_CODE);
                } else {
                    //Runs When Granted Permisssion
                    EditText iptxt   = (EditText)findViewById(R.id.ip);
                    EditText porttxt   = (EditText)findViewById(R.id.port);
                    String ip = iptxt.getText().toString();
                    int port = new Integer(porttxt.getText().toString()).intValue();
                    if (!sendImage.verifyIP(ip)){
                        Toast toast = Toast.makeText(v.getContext(), "No valid IP!", Toast.LENGTH_LONG);
                        toast.show();
                    }
                    else if(port>65535){
                        Toast toast = Toast.makeText(v.getContext(), "No valid Port!", Toast.LENGTH_LONG);
                        toast.show();
                    }
                    else{
                        InputStream picture = null;
                        try {
                            picture = getAssets().open(imageselected);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        if (picture == null){
                            Toast toast = Toast.makeText(v.getContext(), "Could not load image!", Toast.LENGTH_LONG);
                            toast.show();
                        }
                        else{
                            RadioGroup radioGroup = (RadioGroup) findViewById(R.id.selections);
                            int selectedId = radioGroup.getCheckedRadioButtonId();

                            // View
                            RadioButton radioButton = (RadioButton) findViewById(selectedId);

                            // Parameters
                            EditText classtxt   = (EditText)findViewById(R.id.classtype);
                            EditText namemodel   = (EditText)findViewById(R.id.modelname);
                            EditText kernel   = (EditText)findViewById(R.id.kernel_SVM);
                            EditText paramC   = (EditText)findViewById(R.id.Cparam_SVM);


                            String params ="";
                            boolean imgsend = false;
                            if(radioButton.getText().toString() == v.getContext().getString(R.string.data_add)){


                                params="data";
                                params+=":"+classtxt.getText().toString();
                                imgsend=true;
                            }
                            else if(radioButton.getText().toString() == v.getContext().getString(R.string.testing)){
                                params="test";
                                if(namemodel.getText().toString()!=""){
                                    params+=":"+namemodel.getText().toString();
                                }
                                else{
                                    params+=":"+"model_default";
                                }
                                if(classtxt.getText().toString()!=""){
                                    params+=":"+classtxt.getText().toString();
                                }
                                imgsend=true;
                                // get result from server in thread
                                if (receiverThread==null){
                                    Log.d("AGAIN", ip);
                                    receiverThread = new Client(ip, port+2, MainActivity.this);
                                    receiverThread.start();
                                }
                            }
                            else if(radioButton.getText().toString() == v.getContext().getString(R.string.training)){
                                params="train";
                                // TODO More parameters? Validate SVM and more!

                                if(namemodel.getText().toString()!=""){
                                    params+=":"+namemodel.getText().toString();
                                }
                                else{
                                    params+=":"+"model_default";
                                }
                                if(kernel.getText().toString()!=""){
                                    params+=":"+kernel.getText().toString();
                                }
                                else{
                                    params+=":"+"linear";
                                }
                                if(paramC.getText().toString()!=""){
                                    params+=":"+paramC.getText().toString();
                                }
                                else{
                                    params+=":"+"1";
                                }
                            }
                            sendCommand sendCMD = new sendCommand(ip,port,params); //different ports for command and image!
                            sendCMD.execute();
                            if(imgsend){
                                sendImage sendIMG = new sendImage(ip,port+1,picture); //different ports for command and image!
                                sendIMG.execute();
                            }
                        }
                    }
                }

            }
        });

        Button click2 = (Button)findViewById(R.id.buttonimg);

        //Sets Up OnClick Listener For Button
        click2.setOnClickListener(new View.OnClickListener() {


            @Override
            public void onClick(View v) {
                //Activly Requests Acsess To Files (With Pop-Up)
                ImageView view = (ImageView)findViewById(R.id.imageView2);
                // Display next image
                countimage+=1;
                if(countimage>=displayedImage.size()){
                    countimage=0;
                }
                imageselected=displayedImage.get(countimage);
                switch (countimage){
                    case 0:
                        view.setImageResource(R.drawable.android_studio_cat);
                        break;
                    case 1:
                        view.setImageResource(R.drawable.refpect_android);
                        break;
                    case 2:
                        view.setImageResource(R.drawable.grumpycat);
                        break;
                    case 3:
                        view.setImageResource(R.drawable.shiba_inu);
                        break;
                    default:
                        view.setImageResource(R.drawable.refpect_android);
                        countimage=0;
                        imageselected=displayedImage.get(countimage);
                        break;
                }
            }
        });

        RadioGroup radioGroup = (RadioGroup) findViewById(R.id.selections);
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                // checkedId is the RadioButton selected
                RadioButton radioButton = (RadioButton) findViewById(checkedId);

                LinearLayout layout_vis = (LinearLayout) findViewById(R.id.modelset);

                LinearLayout layout_model = (LinearLayout) findViewById(R.id.svm_model);
                LinearLayout layout_kernel = (LinearLayout) findViewById(R.id.svm_kernel);
                LinearLayout layout_cparam = (LinearLayout) findViewById(R.id.svm_c);
                LinearLayout layout_label = (LinearLayout) findViewById(R.id.classlayout);

                Button click = (Button)findViewById(R.id.button);

                LinearLayout layout_vis_class = (LinearLayout) findViewById(R.id.classlayout);

                if(radioButton.getText().toString() == getString(R.string.training)){
                    //layout_vis.setVisibility(View.VISIBLE);
                    click.setText("Train SVM");
                    //layout_vis_class.setVisibility(View.GONE);

                    layout_model.setVisibility(View.VISIBLE);
                    layout_kernel.setVisibility(View.VISIBLE);
                    layout_cparam.setVisibility(View.VISIBLE);
                    layout_label.setVisibility(View.GONE);


                }
                if(radioButton.getText().toString() == getString(R.string.testing)){
                    click.setText("Test Image on Server");

                    layout_model.setVisibility(View.VISIBLE);
                    layout_kernel.setVisibility(View.GONE);
                    layout_cparam.setVisibility(View.GONE);
                    layout_label.setVisibility(View.VISIBLE);
                }
                else if(radioButton.getText().toString() == getString(R.string.data_add)){
                    click.setText("Add Image to Dataset on Server");

                    layout_model.setVisibility(View.GONE);
                    layout_kernel.setVisibility(View.GONE);
                    layout_cparam.setVisibility(View.GONE);
                    layout_label.setVisibility(View.VISIBLE);
                }
            }
        });

    }

    public static MainActivity getApp(){
        return (MainActivity) context;
    }

    public Handler getMainThreadHandler() {
        if (mainThreadHandler == null) {
            mainThreadHandler = new Handler(Looper.getMainLooper());
        }
        return mainThreadHandler;
    }

    /**
     * Thread safe way of displaying toast.
     * @param message
     * @param duration
     */
    public void showToast(final String message, final int duration) {
        getMainThreadHandler().post(new Runnable() {
            @Override
            public void run() {
                if (!TextUtils.isEmpty(message)) {
                    toast = Toast.makeText(MainActivity.this, message, duration);
                    /*if (toast != null) {
                        toast.cancel(); //dismiss current toast if visible
                        toast.setText(message);
                    } else {
                        toast = Toast.makeText(MainActivity.this, message, duration);
                    }*/
                    toast.show();
                }
            }
        });
    }
}

//Main Actions - Asynchronous
class sendImage extends AsyncTask<Void,Object,Void> {

    // IP veryfier
    private static final String IPV4_REGEX =
            "^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                    "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                    "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                    "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
    private static final Pattern IPv4_PATTERN = Pattern.compile(IPV4_REGEX);

    static Socket s; //Socket Variable
    private String ip;
    private int port;
    private InputStream picture;
    public sendImage(String ip, int port, InputStream picture){

        this.ip = ip;
        this.port = port;
        this.picture = picture;
    }

    @Override
    protected Void doInBackground(Void...params){
        try {
            if(verifyIP(ip) || port<=65535){
                if (pingIP(ip)){
                    s = new Socket(ip,port); //Connects to IP address - enter your IP here
                    try {

                        try {
                            //Reads bytes (all together)

                            int bytesRead;
                            while ((bytesRead = picture.read()) != -1) {
                                s.getOutputStream().write(bytesRead); //Writes bytes to output stream
                            }
                        } finally {
                            //Flushes and closes socket
                            s.getOutputStream().flush();
                            s.close();
                        }
                    } finally {
                        picture.close();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;

    }

    // Verify if string is an IP
    public static boolean verifyIP(String ip) {
        if (ip == null) return false;

        Matcher matcher = IPv4_PATTERN.matcher(ip);
        return matcher.matches();
    }

    // Ping an IP (maybe use runnable for not freezing UI)
    public static boolean pingIP(String host_new) {
        //System.out.println("executeCommand");
        if (verifyIP(host_new)) {
            Runtime runtime = Runtime.getRuntime();
            try {
                Process mIpAddrProcess = runtime.exec("/system/bin/ping -c 1 " + host_new);
                boolean mExitValue = false;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    mExitValue = mIpAddrProcess.waitFor(5, TimeUnit.SECONDS);
                } else {
                    int mExitValueold = mIpAddrProcess.waitFor();
                    if (mExitValueold == 0) {
                        mExitValue = true;
                    } else {
                        mExitValue = false;
                    }
                }
                return mExitValue;
                //System.out.println(" mExitValue "+mExitValue);
                /*if(!mExitValue){
                    return true;
                }else{
                    return false;
                }*/
            } catch (InterruptedException ignore) {
                ignore.printStackTrace();
                //System.out.println(" Exception:"+ignore);
            } catch (IOException e) {
                e.printStackTrace();
                // System.out.println(" Exception:"+e);
            }
        }
        return false;
    }

}

//Main Actions to send commands to server - Asynchronous
class sendCommand extends AsyncTask<Void,Object,Void> {

    // IP veryfier
    private static final String IPV4_REGEX =
            "^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                    "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                    "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                    "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
    private static final Pattern IPv4_PATTERN = Pattern.compile(IPV4_REGEX);

    static Socket s; //Socket Variable
    private String ip;
    private int port;
    private String params;

    public sendCommand(String ip, int port, String params){

        this.ip = ip;
        this.port = port;
        this.params = params;
    }

    @Override
    protected Void doInBackground(Void...noparams){
        try {
            if(verifyIP(ip) || port<=65535){
                if (pingIP(ip)){
                    try {
                        s = new Socket(ip, port);
                        /*OutputStreamWriter osw =new OutputStreamWriter(s.getOutputStream(), "UTF-8");
                        osw.write(params, 0, params.length());*/
                       /* DataOutputStream dataOutputStream = new DataOutputStream(s.getOutputStream());
                        dataOutputStream.writeUTF(params); */
                        PrintStream out = new PrintStream(s.getOutputStream());
                        out.println(params);

                    } catch (IOException e) {
                        System.err.print(e);
                    }finally {
                        s.close();
                    }

                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;

    }

    // Verify if string is an IP
    public static boolean verifyIP(String ip) {
        if (ip == null) return false;

        Matcher matcher = IPv4_PATTERN.matcher(ip);
        return matcher.matches();
    }

    // Ping an IP (maybe use runnable for not freezing UI)
    public static boolean pingIP(String host_new) {
        //System.out.println("executeCommand");
        if (verifyIP(host_new)) {
            Runtime runtime = Runtime.getRuntime();
            try {
                Process mIpAddrProcess = runtime.exec("/system/bin/ping -c 1 " + host_new);
                boolean mExitValue = false;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    mExitValue = mIpAddrProcess.waitFor(5, TimeUnit.SECONDS);
                } else {
                    int mExitValueold = mIpAddrProcess.waitFor();
                    if (mExitValueold == 0) {
                        mExitValue = true;
                    } else {
                        mExitValue = false;
                    }
                }
                return mExitValue;
                //System.out.println(" mExitValue "+mExitValue);
                /*if(!mExitValue){
                    return true;
                }else{
                    return false;
                }*/
            } catch (InterruptedException ignore) {
                ignore.printStackTrace();
                //System.out.println(" Exception:"+ignore);
            } catch (IOException e) {
                e.printStackTrace();
                // System.out.println(" Exception:"+e);
            }
        }
        return false;
    }
}


// Receive from server
class Client extends Thread {

    private String ip;
    private int port;
    private String params;
    private Context context;

    static ServerSocket s; //Socket Variable
    static Socket clint; //Socket Variable
    public Client(String ip, int port, Activity context){

        this.ip = ip;
        this.port = port;
        this.context = context;
    }

    public void run()
    {
        Log.d("THREAD", ip);
        Log.d("THREAD", String.valueOf(port));
        try {
            boolean receivedCMD = true;
            while (receivedCMD){
                try
                {
                    s = new ServerSocket(port);

                    clint =s.accept();
                    PrintWriter out = new PrintWriter(clint.getOutputStream(), true);

                    BufferedReader in = new BufferedReader(new InputStreamReader(clint.getInputStream()));

                    String response = in.readLine();

                    // Analyze response, Example: [0]:AndroKitten
                    List<String> list = new ArrayList<String>();
                    String substr = response;
                    while(substr.length()>0){
                        if (substr.indexOf(":")!=-1){
                            list.add(substr.substring(0,substr.indexOf(":")));
                            substr=substr.substring(substr.indexOf(":")+1);
                        }
                        else{
                            list.add(substr);
                            substr="";
                        }
                    }

                    //TODO: Catch more commands like SVM training fished
                    MainActivity.getApp().showToast("Predicted class was "+list.get(1)+ " (Number: "+list.get(0)+")", Toast.LENGTH_LONG);
                    s.close();
                    clint.close();
                }
                catch(Exception e) {System.out.println(e);}

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            s.close();
            clint.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}

