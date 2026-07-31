    public class ModelFragment extends Fragment implements Handler.Callback {
       Handler backHandler1, backHandler2;
       @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setRetainInstance(true);
    
            HandlerThread backThread1 = new HandlerThread("BACK_THREAD_1");
            backThread1.start();
            backHandler1 = new Handler(backThread1.getLooper(), this);
    
            HandlerThread backThread2 = new HandlerThread("BACK_THREAD_2");
            backThread2.start();
            backHandler2 = new Handler(backThread2.getLooper(), this);
    
    	backHandler1.obtainMessage(BACK1_WHAT, backObj2).sendToTarget();
        }
    
        @Override
        public boolean handleMessage(Message msg) {
            switch(msg.what){
                case BACK1_WHAT:
                    // this code runs on thread 1
                    backHandler2.obtainMessage(BACK2_WHAT, backObj1).sendToTarget();
                    return true;
    
                case BACK2_WHAT:
    		    // this code runs on thread 2
                    backHandler1.obtainMessage(BACK1_WHAT, backObj2).sendToTarget();
                    return true;
            }
            return false;
        }
    
    }