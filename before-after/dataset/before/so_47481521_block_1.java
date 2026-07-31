    AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    //Do things.
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //Update UI.
                        }
                    });
                    //Do things.
                }
            });