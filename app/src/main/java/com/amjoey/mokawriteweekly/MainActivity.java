package com.amjoey.mokawriteweekly;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.TimePickerDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.util.Calendar;

import Moka7.S7;
import Moka7.S7Client;

public class MainActivity extends FragmentActivity {
    boolean timeView = true;

    int bMemoryValOnVal1,bMemoryValOffVal1;

    static final int START_TIME_ID=0;
    static final int END_TIME_ID=1;

    EditText onVal1,offVal1;

    ImageButton imgOnVal1,imgOffVal1;
    private int chour,cminute;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Calendar calendar=Calendar.getInstance();
        chour=calendar.get(Calendar.HOUR_OF_DAY);
        cminute=calendar.get(Calendar.MINUTE);

        onVal1 = (EditText) findViewById(R.id.txtOnValue1);
        offVal1 = (EditText) findViewById(R.id.txtOffValue1);

        imgOnVal1 = (ImageButton)  findViewById(R.id.imgOnVal1);
        imgOffVal1 = (ImageButton) findViewById(R.id.imgOffVal1);

        imgOnVal1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog(START_TIME_ID);
            }
        });

        imgOffVal1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                showDialog(END_TIME_ID);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // we're going to simulate real time with thread that append data to the graph
        new Thread(new Runnable() {

            @Override
            public void run() {
                // we add 100 new entries
                for (int i = 0; i < 500; i++) {
                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            new PlcReader().execute("");
                        }
                    });

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                    }
                }
            }
        }).start();
    }


    S7Client client = new S7Client();

    public void writedb_val(View v){
        onVal1 = (EditText) findViewById(R.id.txtOnValue1);
        offVal1 = (EditText) findViewById(R.id.txtOffValue1);

        bMemoryValOnVal1=timetoint(onVal1.getText().toString());
        bMemoryValOffVal1=timetoint(offVal1.getText().toString());

        new PlcWriter().execute("");

        timeView =true;
    }

    //begin class PlcReader
    private class PlcReader extends AsyncTask<String, Void, String> {

        String ret= "";
        int timeON1,timeOFF1;

        @Override
        protected String doInBackground(String... params){

            try{
                client.SetConnectionType(S7.S7_BASIC);
                int res=client.ConnectTo("192.168.1.12",0,0);

                if(res==0){//connection OK


                    byte[] data = new byte[12];

                    byte[] data1 = new byte[4];
                    res = client.ReadArea(S7.S7AreaDB,1,1,12,data);

                    // Get time from PLC VM Address 988->Hour 989->Minute 990->Second
                    res = client.ReadArea(S7.S7AreaDB,1,988,3,data1);

                    //  ret = "value of DB1.DBD25: "+ S7.GetFloatAt(data,0);
                    //  ret = "value of DB1.DBD10: "+ S7.GetWordAt(data,0);
                    //ret = "Value of DB1.DBD1: "+S7.GetWordAt(data1,0)/256 +":"+S7.GetWordAt(data1,1)/256 +":"+S7.GetWordAt(data1,2)/256 +"/"+ S7.GetWordAt(data,0)+"/"+ S7.GetWordAt(data,2)+"/"+ S7.GetWordAt(data,4)+"/"+ S7.GetWordAt(data,6)+"/"+ S7.GetWordAt(data,8)+"/"+ S7.GetWordAt(data,10);
                    ret = "PLC Time : "+padding(S7.GetWordAt(data1,0)/256) +":"+padding(S7.GetWordAt(data1,1)/256) +":"+padding(S7.GetWordAt(data1,2)/256) ;

                    timeON1 = S7.GetWordAt(data,0);
                    timeOFF1 = S7.GetWordAt(data,2);
 /*
                    byte[] dataWrite = new byte[2];
                   // S7.SetBitAt(dataWrite, 0, 1, true);
                   // S7.SetDIntAt(dataWrite,0,5);
                    S7.SetWordAt(dataWrite,0,700);

                    client.WriteArea(S7.S7AreaDB, 1, 12, 2, dataWrite);
                    ret = "WriteArea of DB1.DBD12: OK ";
                    */



                }else{
                    ret = "ERR: "+ S7Client.ErrorText(res);
                }
                client.Disconnect();
            }catch (Exception e) {
                ret = "EXC: "+e.toString();
                Thread.interrupted();
            }
            return "executed";
        }

        @Override
        protected void onPostExecute(String result){

            TextView txout = (TextView) findViewById(R.id.textView);
            txout.setText(ret);

            if(timeView) {
                onVal1 = (EditText) findViewById(R.id.txtOnValue1);
                onVal1.setText(timeformat(timeON1));

                offVal1 = (EditText) findViewById(R.id.txtOffValue1);
                offVal1.setText(timeformat(timeOFF1));

                timeView =false;
            }

        }

    }
    //end class PlcReader

//begin class PlcWriter
    private class PlcWriter extends AsyncTask<String, Void, String> {

        String ret= "";


        @Override
        protected String doInBackground(String... params){

            try{
                client.SetConnectionType(S7.S7_BASIC);
                int res=client.ConnectTo("192.168.1.12",0,0);

                if(res==0){//connection OK


                    byte[] dataWrite = new byte[4];
                    // S7.SetBitAt(dataWrite, 0, 1, true);
                    // S7.SetDIntAt(dataWrite,0,5);
                    S7.SetWordAt(dataWrite,0,bMemoryValOnVal1);
                    S7.SetWordAt(dataWrite,2,bMemoryValOffVal1);


                    client.WriteArea(S7.S7AreaDB, 1, 1, 4, dataWrite);

                    ret = "Updated";


                }else{
                    ret = "ERR: "+ S7Client.ErrorText(res);
                }
                client.Disconnect();
            }catch (Exception e) {
                ret = "EXC: "+e.toString();
                Thread.interrupted();
            }
            return "executed";
        }

        @Override
        protected void onPostExecute(String result){

            Context context = getApplicationContext();
            Toast.makeText(context, ret, Toast.LENGTH_LONG).show();

        }

    }
    //end class PlcWriter
/*
    public  void  selectTime (View v){
        DialogFragment newFragment = new TimePickerFragment();
        newFragment.show(getFragmentManager(),"TimePicker");
    }
*/

    private TimePickerDialog.OnTimeSetListener mStartTime=new TimePickerDialog.OnTimeSetListener()
    {
        public void onTimeSet(TimePicker view,int hourofday,int min)
        {
            onVal1.setText(new StringBuilder().append(padding(hourofday))
                    .append(":").append(padding(min)));
        }
    };

    private TimePickerDialog.OnTimeSetListener mEndTime=new TimePickerDialog.OnTimeSetListener()
    {
        public void onTimeSet(TimePicker view,int hourofday,int min)
        {
            offVal1.setText(new StringBuilder().append(padding(hourofday))
                    .append(":").append(padding(min)));
        }
    };

    @Override
    protected Dialog onCreateDialog(int id)
    {
        switch (id)
        {
            case START_TIME_ID:
                return new TimePickerDialog(this,AlertDialog.THEME_HOLO_LIGHT,mStartTime,chour,cminute,false);
            case END_TIME_ID:
                return new TimePickerDialog(this,AlertDialog.THEME_HOLO_LIGHT,mEndTime,chour,cminute,false);
        }
        return null;
    }

    public static String timeformat(int t){
        String intTime;
        intTime =String.valueOf(Integer.toHexString(t));
        String first = padding(Integer.parseInt(intTime.substring(0, intTime.length() / 2)));
        String second = padding(Integer.parseInt(intTime.substring(intTime.length() / 2)));
        return first+ ":"  +second;
    }

    public static int timetoint(String s){
        int setTime;
        String[] separated = s.split(":");

        String first = separated[0];
        String second =separated[1];
        setTime =Integer.parseInt(first+second,16);
        return setTime;
    }

    public static String padding(int c){
        if(c>=10)
            return String.valueOf(c);
        else
            return "0"+ String.valueOf(c);
    }



}
