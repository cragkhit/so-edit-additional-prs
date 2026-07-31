    private Collection<String> getNodes() {
        HashSet <String>results = new HashSet<String>();
        NodeApi.GetConnectedNodesResult nodes =
            Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
        for (Node node : nodes.getNodes()) {
            results.add(node.getId());
        }
        return results;
    }
    
    private String pickBestNodeId(Set<Node> nodes) {
        String bestNodeId = null;
        // Find a nearby node or pick one arbitrarily
        for (Node node : nodes) {
            if (node.isNearby()) {
                return node.getId();
             }
             bestNodeId = node.getId();
        }
        return bestNodeId;
    }
    ...
    GoogleApiClient mGoogleApiClient = new GoogleApiClient.Builder(this)
        .addConnectionCallbacks(new ConnectionCallbacks() {
                @Override
                public void onConnected(Bundle connectionHint) {
                    Log.d(TAG, "onConnected: " + connectionHint);
                    // Now you can use the Data Layer API
                }
                @Override
                public void onConnectionSuspended(int cause) {
                    Log.d(TAG, "onConnectionSuspended: " + cause);
                }
        })
        .addOnConnectionFailedListener(new OnConnectionFailedListener() {
                @Override
                public void onConnectionFailed(ConnectionResult result) {
                    Log.d(TAG, "onConnectionFailed: " + result);
                }
            })
        // Request access only to the Wearable API
        .addApi(Wearable.API)
        .build();
    mGoogleApiClient.blockingConnect();
    Channel channel = openChannel(mGoogleApiClient, pickBestNodeId(getNodes()), "Your arbitrary application specific path").await();
    channel.sendFile(mGoogleApiClient, yourFileAsUri, 0/*start offset*/, yourFileLength/*end*/);
    channel.close(mGoogleApiClient);
    ...