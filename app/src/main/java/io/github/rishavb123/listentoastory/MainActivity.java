package io.github.rishavb123.listentoastory;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    private static final int SMS_PERMISSION_REQUEST_CODE = 1000;
    private BroadcastReceiver broadcastReceiver;
    private Bot bot;

    @Override
    protected void onResume() {
        super.onResume();
        bot = new Bot( (TextView) findViewById(R.id.textView), (TextView) findViewById(R.id.textView2), (ListView) findViewById(R.id.listView), (SeekBar) findViewById(R.id.seekBar));

        if(checkSelfPermission(Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED || checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS, Manifest.permission.SEND_SMS}, SMS_PERMISSION_REQUEST_CODE);
        else {
            registerBroadcast();
        }
    }

    public void registerBroadcast()
    {
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Bundle bundle = intent.getExtras();
                assert bundle != null;
                Object[] pdus = (Object[]) bundle.get("pdus");
                assert pdus != null;
                SmsMessage[] smsMessages = new SmsMessage[pdus.length];
                for(int i = 0; i < pdus.length; i++)
                    smsMessages[i] = SmsMessage.createFromPdu((byte[]) pdus[i], bundle.getString("format"));
                bot.receiveMessage(smsMessages[0].getOriginatingAddress(), smsMessages[0].getMessageBody());
            }
        };
        registerReceiver(broadcastReceiver, new IntentFilter("android.provider.Telephony.SMS_RECEIVED"));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(broadcastReceiver);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(grantResults.length == 0)
            return;

        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            if (SMS_PERMISSION_REQUEST_CODE == requestCode) {
                registerBroadcast();;
            }

        } else if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
            if (SMS_PERMISSION_REQUEST_CODE == requestCode) {
                Toast.makeText(this, "Please allow permissions t        View adapterLayout = layoutInflater.inflate(resource, null);\no sms", Toast.LENGTH_SHORT).show();
                requestPermissions(new String[]{Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS, Manifest.permission.SEND_SMS}, SMS_PERMISSION_REQUEST_CODE);
            }
        }

    }

    class Bot {

        TextView textView;
        TextView stateView;

        SmsManager smsManager;
        Handler handler;
        int delayPerCharacter;

        ArrayList<String> story;
        ArrayAdapter<String> adapter;

        int state;

        int spammingCount = 0;
        int previousState;

        private static final int INITIAL_STATE = 885;
        private static final int GREETING_STATE = 487;
        private static final int LISTENING_STATE = 852;
        private static final int LAUGHING_STATE = 954;
        private static final int SPAMMING_STATE = 744;
        private static final int ANSWER_STATE = 799;
        private static final int BYE_STATE = 919;
        private static final int CONFUSED_STATE = 407;
        private static final int END_STORY_STATE = 182;

        HashMap<Integer, String[]> responses;

        Bot(TextView textView, TextView stateView, ListView listView, SeekBar seekBar)
        {
            this.textView = textView;
            this.stateView = stateView;

            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    delayPerCharacter = 1000 - progress;
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });

            seekBar.setProgress(700);

            smsManager = SmsManager.getDefault();
            handler = new Handler();
            setState(INITIAL_STATE);

            story = new ArrayList<>();
            responses = new HashMap<>();

            adapter = new ArrayAdapter<String>(getApplicationContext(), R.layout.list_item, story) {
                @NonNull
                @Override
                public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                    LayoutInflater layoutInflater = (LayoutInflater) getApplicationContext().getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
                    View adapterLayout = layoutInflater.inflate(R.layout.list_item, null);
                    ((TextView) adapterLayout.findViewById(R.id.textView3)).setText(getItem(position));
                    return adapterLayout;
                }
            };

            listView.setAdapter(adapter);

            responses.put(INITIAL_STATE, new String[]{null});
            responses.put(GREETING_STATE, new String[]{"Hey", "Hi", "Hello", "Greetings"});
            responses.put(LISTENING_STATE, new String[]{"Uhuh", "Ok", "okay", "alright", "keeping going"});
            responses.put(LAUGHING_STATE, new String[]{"LOL", "lol", "OMG LOL", "LoL", "lOl", "LAUGHING OUT LOUD", "LOL I'M DYING"});
            responses.put(SPAMMING_STATE, new String[]{"Hi", "Hello", "I am spamming", "I am spamming", "Dont block me", "A", "This is going to get annoying fast"});
            responses.put(ANSWER_STATE, new String[]{"Ya", "Ya sure", "Sure", "Of course"});
            responses.put(END_STORY_STATE, new String[] {"great story I loved it", "nice story", "cool", "lol no offense but I didn't really need to hear that", "you just wasted a bunch of my time lol"});
            responses.put(BYE_STATE, new String[]{"Cya", "See ya later", "Bye", "ttyl"});
            responses.put(CONFUSED_STATE, new String[]{"I don't rlly get what ur saying lol", "um wut?", "Now Im confused", "I am confusion"});

        }

        String getStateString(int state) {
            switch(state)
            {
                case INITIAL_STATE:
                    return "INITIAL_STATE";

                case GREETING_STATE:
                    return "GREETING_STATE";

                case LISTENING_STATE:
                    return "LISTENING_STATE";

                case LAUGHING_STATE:
                    return "LAUGHING_STATE";

                case SPAMMING_STATE:
                    return "SPAMMING_STATE";

                case ANSWER_STATE:
                    return "ANSWER_STATE";

                case END_STORY_STATE:
                    return "END_STORY_STATE";

                case BYE_STATE:
                    return "BYE_STATE";

                case CONFUSED_STATE:
                    return "CONFUSED_STATE";
            }
            return "NO_STATE";
        }

        String createMessage() {
            return responses.get(state)[(int)(Math.random()*responses.get(state).length)];
        }

        void receiveMessage(String originatingAdressing, String message) {
            chooseState(originatingAdressing, message.toLowerCase());
            sendMessage(originatingAdressing, createMessage(), true);
        }

        void chooseState(String originatingAdress, String message) {

            if (message.indexOf("spam") > -1) {
                sendMessage(originatingAdress, "Oh you wanna see spam?", false);
                setState(SPAMMING_STATE);
                return;
            }

            switch(state)
            {
                case INITIAL_STATE:
                    if(message.indexOf("hi") + message.indexOf("hey") + message.indexOf("hello") + message.indexOf("yo") > -4) {
                        textView.setText("Greeting Detected . . .");
                        setState(GREETING_STATE);
                    }
                    break;

                case GREETING_STATE:
                    if(message.indexOf("do you") + message.indexOf("wanna") > -2 && message.indexOf("story") > -1) {
                        textView.setText("He/She wants to tell a story . . .");
                        setState(ANSWER_STATE);
                    } else if(!(message.indexOf("hi") + message.indexOf("hey") + message.indexOf("hello") + message.indexOf("yo") > -4)){
                        textView.setText("Not sure what he/she is doing");
                        setState(CONFUSED_STATE);
                    }
                    break;

                case ANSWER_STATE:
                    textView.setText("He/She is telling the story . . .");
                    story.add(message);
                    adapter.notifyDataSetChanged();
                    setState(LISTENING_STATE);
                    break;

                case LISTENING_STATE:
                    story.add(message);
                    adapter.notifyDataSetChanged();
                    if(message.indexOf("the end") + message.indexOf("thats it") + message.indexOf("I'm done") > -3) {
                        textView.setText("He/She finished the story . . .");
                        setState(END_STORY_STATE);
                    } else if(message.indexOf("lol") + message.indexOf("funny") + message.indexOf("hilarious") > -3) {
                        textView.setText("He/She said something funny . . .");
                        setState(LAUGHING_STATE);
                    }
                    break;

                case LAUGHING_STATE:
                    setState(LISTENING_STATE);
                    chooseState(originatingAdress, message);
                    break;

                case SPAMMING_STATE:
                    setState(previousState);
                    chooseState(originatingAdress, message);
                    break;

                case CONFUSED_STATE:
                    setState(previousState);
                    chooseState(originatingAdress, message);
                    break;

                case END_STORY_STATE:
                    if(message.indexOf("bye") + message.indexOf("cya") + message.indexOf("gtg") + message.indexOf("ttyl") > -4)
                    {
                        textView.setText("He/She is leaving . . .");
                        setState(BYE_STATE);
                    } else {
                        textView.setText("Not sure what he/she is doing");
                        setState(CONFUSED_STATE);
                    }
                    break;

                case BYE_STATE:
                    setState(END_STORY_STATE);
                    chooseState(originatingAdress, message);
                    break;

            }
        }

        void setState(int state) {
            if(state == SPAMMING_STATE || state == CONFUSED_STATE)
                previousState = this.state;
            this.state = state;
            if(state == SPAMMING_STATE)
                spammingCount = (int)(Math.random()*5) + 5;
            stateView.setText(getStateString(this.state));
        }

        void sendMessage(final String sendTo, final String message, boolean haveDelay) {
            if(message == null)
                return;
            if(haveDelay) {
                int delay = message.length() * delayPerCharacter;
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        smsManager.sendTextMessage(sendTo, null, message, null, null);
                        if (state == SPAMMING_STATE && spammingCount >= 0) {
                            spammingCount--;
                            sendMessage(sendTo, createMessage(), true);
                        }
                    }
                }, delay);


            } else {
                smsManager.sendTextMessage(sendTo, null, message, null, null);
            }
        }

    }

}