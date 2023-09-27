package flwr.android_client;

import android.app.Activity;
import android.icu.text.SimpleDateFormat;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.util.Pair;
import android.util.Patterns;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import  flwr.android_client.FlowerServiceGrpc.FlowerServiceStub;
import com.google.protobuf.ByteString;

import io.grpc.stub.StreamObserver;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class MainActivity extends AppCompatActivity {
    private EditText ip;
    private EditText port;
    private Button loadDataButton;
    private Button connectButton;
    private Button trainButton;
    private TextView resultText;
    private EditText device_id;
    private ManagedChannel channel;
    public FlowerClient fc;
    private static final String TAG = "Flower";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        resultText = (TextView) findViewById(R.id.grpc_response_text);
        resultText.setMovementMethod(new ScrollingMovementMethod());
        device_id = (EditText) findViewById(R.id.device_id_edit_text);
        ip = (EditText) findViewById(R.id.serverIP);
        port = (EditText) findViewById(R.id.serverPort);
        loadDataButton = (Button) findViewById(R.id.load_data) ;
        connectButton = (Button) findViewById(R.id.connect);
        trainButton = (Button) findViewById(R.id.trainFederated);

<<<<<<< Updated upstream
        fc = new FlowerClient(this);
=======
        messageAdapter = new MessageAdapter(readStringFromFile( getApplicationContext() , "FlowerResults.txt")); // Create your custom adapter
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(messageAdapter);

        requestPermission();

        LifecycleOwner lifecycleOwner = this ;
        WorkManager.getInstance(getApplicationContext()).getWorkInfosForUniqueWorkLiveData("my_unique_periodic_work").observe(lifecycleOwner, new Observer<List<WorkInfo>>() {
            @Override
            public void onChanged(List<WorkInfo> workInfos) {
                if (workInfos.size() > 0) {
                    WorkInfo info = workInfos.get(0);
                    // You can recieve any message from the Worker Thread
                    refreshRecyclerView();
                }
            }
        });
        // code for functionality of permission buttons :
        batteryOptimisationButton = findViewById(R.id.battery_optimisation);
        batteryOptimisationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleBatteryOptimization();
            }
        });
>>>>>>> Stashed changes
    }

    public static void hideKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        View view = activity.getCurrentFocus();
        if (view == null) {
            view = new View(activity);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }


    public void setResultText(String text) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss", Locale.GERMANY);
        String time = dateFormat.format(new Date());
        resultText.append("\n" + time + "   " + text);
    }

    public void loadData(View view){
        if (TextUtils.isEmpty(device_id.getText().toString())) {
            Toast.makeText(this, "Please enter a client partition ID between 1 and 10 (inclusive)", Toast.LENGTH_LONG).show();
        }
        else if (Integer.parseInt(device_id.getText().toString()) > 10 ||  Integer.parseInt(device_id.getText().toString()) < 1)
        {
            Toast.makeText(this, "Please enter a client partition ID between 1 and 10 (inclusive)", Toast.LENGTH_LONG).show();
        }
        else{
            hideKeyboard(this);
            setResultText("Loading the local training dataset in memory. It will take several seconds.");
            loadDataButton.setEnabled(false);

            ExecutorService executor = Executors.newSingleThreadExecutor();
            Handler handler = new Handler(Looper.getMainLooper());

            executor.execute(new Runnable() {
                private String result;
                @Override
                public void run() {
                    try {
                        fc.loadData(Integer.parseInt(device_id.getText().toString()));
                        result =  "Training dataset is loaded in memory.";
                    } catch (Exception e) {
                        StringWriter sw = new StringWriter();
                        PrintWriter pw = new PrintWriter(sw);
                        e.printStackTrace(pw);
                        pw.flush();
                        result =  "Training dataset is loaded in memory.";
                    }
                    handler.post(() -> {
                        setResultText(result);
                        connectButton.setEnabled(true);
                    });
                }
            });
        }
    }

    public void connect(View view) {
        String host = ip.getText().toString();
        String portStr = port.getText().toString();
        if (TextUtils.isEmpty(host) || TextUtils.isEmpty(portStr) || !Patterns.IP_ADDRESS.matcher(host).matches()) {
            Toast.makeText(this, "Please enter the correct IP and port of the FL server", Toast.LENGTH_LONG).show();
        }
        else {
            int port = TextUtils.isEmpty(portStr) ? 0 : Integer.parseInt(portStr);
            channel = ManagedChannelBuilder.forAddress(host, port).maxInboundMessageSize(10 * 1024 * 1024).usePlaintext().build();
            hideKeyboard(this);
            trainButton.setEnabled(true);
            connectButton.setEnabled(false);
            setResultText("Channel object created. Ready to train!");
        }
    }

    public void runGrpc(View view){
        MainActivity activity = this;
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(new Runnable() {
            private String result;
            @Override
            public void run() {
                try {
                    (new FlowerServiceRunnable()).run(FlowerServiceGrpc.newStub(channel), activity);
                    result =  "Connection to the FL server successful \n";
                } catch (Exception e) {
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    e.printStackTrace(pw);
                    pw.flush();
                    result = "Failed to connect to the FL server \n" + sw;
                }
                handler.post(() -> {
                    setResultText(result);
                    trainButton.setEnabled(false);
                });
            }
        });
    }


    private static class FlowerServiceRunnable{
        protected Throwable failed;
        private StreamObserver<ClientMessage> requestObserver;

        public void run(FlowerServiceStub asyncStub, MainActivity activity) {
             join(asyncStub, activity);
        }

        private void join(FlowerServiceStub asyncStub, MainActivity activity)
                throws RuntimeException {

            final CountDownLatch finishLatch = new CountDownLatch(1);
            requestObserver = asyncStub.join(
                            new StreamObserver<ServerMessage>() {
                                @Override
                                public void onNext(ServerMessage msg) {
                                    handleMessage(msg, activity);
                                }

                                @Override
                                public void onError(Throwable t) {
                                    t.printStackTrace();
                                    failed = t;
                                    finishLatch.countDown();
                                    Log.e(TAG, t.getMessage());
                                }

                                @Override
                                public void onCompleted() {
                                    finishLatch.countDown();
                                    Log.e(TAG, "Done");
                                }
                            });
        }

        private void handleMessage(ServerMessage message, MainActivity activity) {

            try {
                ByteBuffer[] weights;
                ClientMessage c = null;

                if (message.hasGetParametersIns()) {
                    Log.e(TAG, "Handling GetParameters");
                    activity.setResultText("Handling GetParameters message from the server.");

                    weights = activity.fc.getWeights();
                    c = weightsAsProto(weights);
                } else if (message.hasFitIns()) {
                    Log.e(TAG, "Handling FitIns");
                    activity.setResultText("Handling Fit request from the server.");

                    List<ByteString> layers = message.getFitIns().getParameters().getTensorsList();

                    Scalar epoch_config = message.getFitIns().getConfigMap().getOrDefault("local_epochs", Scalar.newBuilder().setSint64(1).build());

                    assert epoch_config != null;
                    int local_epochs = (int) epoch_config.getSint64();

                    // Our model has 10 layers
                    ByteBuffer[] newWeights = new ByteBuffer[10] ;
                    for (int i = 0; i < 10; i++) {
                        newWeights[i] = ByteBuffer.wrap(layers.get(i).toByteArray());
                    }

                    Pair<ByteBuffer[], Integer> outputs = activity.fc.fit(newWeights, local_epochs);
                    c = fitResAsProto(outputs.first, outputs.second);
                } else if (message.hasEvaluateIns()) {
                    Log.e(TAG, "Handling EvaluateIns");
                    activity.setResultText("Handling Evaluate request from the server");

                    List<ByteString> layers = message.getEvaluateIns().getParameters().getTensorsList();

                    // Our model has 10 layers
                    ByteBuffer[] newWeights = new ByteBuffer[10] ;
                    for (int i = 0; i < 10; i++) {
                        newWeights[i] = ByteBuffer.wrap(layers.get(i).toByteArray());
                    }
                    Pair<Pair<Float, Float>, Integer> inference = activity.fc.evaluate(newWeights);

                    float loss = inference.first.first;
                    float accuracy = inference.first.second;
                    activity.setResultText("Test Accuracy after this round = " + accuracy);
                    int test_size = inference.second;
                    c = evaluateResAsProto(loss, test_size);
                }
                requestObserver.onNext(c);
                activity.setResultText("Response sent to the server");
            }
<<<<<<< Updated upstream
            catch (Exception e){
                Log.e(TAG, e.getMessage());
=======
        } catch (IOException e) {
            e.printStackTrace(); // Handle the exception as needed
        }
    }



    public void startWorker(View view) {

        // ensuring all inputs are entered :

        EditText deviceIdEditText = findViewById(R.id.device_id_edit_text);
        EditText serverIPEditText = findViewById(R.id.serverIP);
        EditText serverPortEditText = findViewById(R.id.serverPort);

        // Get the text from the EditText widgets
        String dataSlice = deviceIdEditText.getText().toString();
        String serverIP = serverIPEditText.getText().toString();
        String serverPort = serverPortEditText.getText().toString();

        if (TextUtils.isEmpty(dataSlice) || TextUtils.isEmpty(serverIP) || TextUtils.isEmpty(serverPort)) {
            // Display a toast message indicating that fields are omitted
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
        } else {

            // Launching the Worker :
            Constraints constraints = new Constraints.Builder()
                    // Add constraints if needed (e.g., network connectivity)
                    .build();

            PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
                    MyWorker.class, 15, TimeUnit.MINUTES)
                    .setInitialDelay(0, TimeUnit.MILLISECONDS)
                    .setInputData(new Data.Builder()
                            .putString( "dataslice", deviceIdEditText.getText().toString() )
                            .putString( "server", serverIPEditText.getText().toString())
                            .putString( "port" , serverPortEditText.getText().toString())
                            .build())
                    .setConstraints(constraints)
                    .build();

            String uniqueWorkName = "my_unique_periodic_work";

            WorkManager.getInstance(getApplicationContext())
                    .enqueueUniquePeriodicWork(uniqueWorkName, ExistingPeriodicWorkPolicy.KEEP, workRequest);

            // Providing user feedback, e.g., a toast message
            Toast.makeText(this, "Worker started!", Toast.LENGTH_SHORT).show();
        }
    }

    // Listener function for the "Stop" button
    public void stopWorker(View view) {
        // Cancel the worker
        WorkManager.getInstance(getApplicationContext()).cancelAllWork();
        // Providing user feedback again, e.g., a toast message
        Toast.makeText(this, "Worker stopped!", Toast.LENGTH_SHORT).show();
    }


    // Another Listener function to refresh the updates :

    public void refresh(View view)
    {
        refreshRecyclerView();
    }

    // Another Listener to clear the contents of the File :

    public void clear(View view)
    {
        clearFileContents(getApplicationContext() , "FlowerResults.txt");
    }


    private void refreshRecyclerView() {
        // Get messages from MessageRepository using the getMessagesArray method
        List<String> messages = readStringFromFile( getApplicationContext() ,"FlowerResults.txt");

        // Update the data source of your adapter with the new messages
        messageAdapter.setData(messages);

        // Notify the adapter that the data has changed
        messageAdapter.notifyDataSetChanged();

    }



    // following code is for just the permission access from the user :
    private void toggleBatteryOptimization() {
        if (isBatteryOptimizationEnabled()) {
            disableBatteryOptimization();
        } else {
            requestBatteryOptimization();
        }
    }

    private boolean isBatteryOptimizationEnabled() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String packageName = getPackageName();
            PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            return powerManager.isIgnoringBatteryOptimizations(packageName);
        }
        // Battery optimization is not available on versions prior to M, so return false.
        return false;
    }

    private void disableBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
            startActivity(intent);
        }
    }

    private void requestBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
//            startActivityForResult(intent, BATTERY_OPTIMIZATION_REQUEST_CODE);
        }
    }

    public void createEmptyFile(String fileName) {
        try {
            File file = new File(fileName);

            // Create the file if it doesn't exist
            if (!file.exists()) {
                file.createNewFile();
>>>>>>> Stashed changes
            }
        }
    }

    private static ClientMessage weightsAsProto(ByteBuffer[] weights){
        List<ByteString> layers = new ArrayList<>();
        for (ByteBuffer weight : weights) {
            layers.add(ByteString.copyFrom(weight));
        }
        Parameters p = Parameters.newBuilder().addAllTensors(layers).setTensorType("ND").build();
        ClientMessage.GetParametersRes res = ClientMessage.GetParametersRes.newBuilder().setParameters(p).build();
        return ClientMessage.newBuilder().setGetParametersRes(res).build();
    }

    private static ClientMessage fitResAsProto(ByteBuffer[] weights, int training_size){
        List<ByteString> layers = new ArrayList<>();
        for (ByteBuffer weight : weights) {
            layers.add(ByteString.copyFrom(weight));
        }
        Parameters p = Parameters.newBuilder().addAllTensors(layers).setTensorType("ND").build();
        ClientMessage.FitRes res = ClientMessage.FitRes.newBuilder().setParameters(p).setNumExamples(training_size).build();
        return ClientMessage.newBuilder().setFitRes(res).build();
    }

    private static ClientMessage evaluateResAsProto(float accuracy, int testing_size){
        ClientMessage.EvaluateRes res = ClientMessage.EvaluateRes.newBuilder().setLoss(accuracy).setNumExamples(testing_size).build();
        return ClientMessage.newBuilder().setEvaluateRes(res).build();
    }
}
