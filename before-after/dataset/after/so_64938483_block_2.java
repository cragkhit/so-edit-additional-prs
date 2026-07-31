    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rvEvents = view.findViewById(R.id.rvEvents);
        //set a layout manager on RV
        rvEvents.setLayoutManager(new LinearLayoutManager(getContext()));
        //set the adapter on the recycler view
        rvEvents.setAdapter(eventAdapter);
        amplifyAndSetAdapter();
        //for some reason the code only works with both notifyDataSetChanged (other is in the other thread)
        eventAdapter.notifyDataSetChanged();
    }
  
    private void amplifyAndSetAdapter() {
        initializeAmplify();
        amplifyQuery();
    }
    private void initializeAmplify() {
        try {
            // Add these lines to add the AWSApiPlugin plugins
            Amplify.addPlugin(new AWSApiPlugin());
            Amplify.configure(getApplicationContext());
            Log.i(TAG, "Initialized Amplify");
        } catch (AmplifyException error) {
            Log.e(TAG, "Could not initialize Amplify", error);
        }
    }
    private void amplifyQuery() {
        Amplify.API.query(
                ModelQuery.list(Event.class),
                response -> {
                    for (Event event : response.getData()) {
                        Log.i("Amplify", "Title: " + event.getEventTitle() + " Date: " + event.getEventDate() + " Time: " + event.getEventTime()
                                + " PostURL: " + event.getPostUrl() + " ExtraInfo: " + event.getExtraInfo() + " Venue: " + event.getVenue() + " Template: " + event.getTemplate());
                        addEvents(event);
                    }
                    //Used to seperate the ui using a wait function to add the proper time delay so that an error isnt thrown 
                    Thread thread = new Thread(){
                        @Override
                        public void run() {
                            try {
                                synchronized (this) {
                                    wait(100);
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            eventAdapter.notifyDataSetChanged();
                                        }
                                    });
                                }
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }};
                    };
                    thread.start();
                },
                error -> Log.e("Amplify", "Query failure", error)
        );
    }
    private void addEvents(Event event) {
        try {
            events.add(event);
            Log.i(TAG, "Events: " + events.size());
        } catch (Exception e) {
            Log.e(TAG, "Events: ", e);
        }
    }
}