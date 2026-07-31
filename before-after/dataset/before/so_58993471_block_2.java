    // Alice uses Sara's public key
        Key as = aliceKeyAgree.doPhase(saraKpair.getPublic(), false);
    // Bob uses Alice's public key
        Key ba = bobKeyAgree.doPhase(aliceKpair.getPublic(), false);
    // Carol uses Bob's public key
        Key cb = carolKeyAgree.doPhase(bobKpair.getPublic(), false);
    //Sara uses Carol's public key
        Key sc = saraKeyAgree.doPhase(carolKpair.getPublic(), false);
    // Alice uses Sara's result from above
        aliceKeyAgree.doPhase(sc, true);
    // Bob uses Alice's result from above
        bobKeyAgree.doPhase(as, true);
    // Carol uses Bob's result from above
        carolKeyAgree.doPhase(ba, true);
    // Sara uses Carol's result from above
        saraKeyAgree.doPhase(cb, true);
    // Alice, Bob, Carol and Sara compute their secrets
        byte[] aliceSharedSecret = aliceKeyAgree.generateSecret();
        System.out.println("Alice secret: " + toHexString(aliceSharedSecret));
        byte[] bobSharedSecret = bobKeyAgree.generateSecret();
        System.out.println("Bob secret: " + toHexString(bobSharedSecret));
        byte[] carolSharedSecret = carolKeyAgree.generateSecret();
        System.out.println("Carol secret: " + toHexString(carolSharedSecret));
        byte[] saraSharedSecret = saraKeyAgree.generateSecret();        
        System.out.println("Sara secret: " + toHexString(saraSharedSecret));
    // Compare Alice and Bob
        if (!java.util.Arrays.equals(aliceSharedSecret, bobSharedSecret))
            throw new Exception("Alice and Bob differ");
        System.out.println("Alice and Bob are the same");
    // Compare Bob and Carol
        if (!java.util.Arrays.equals(bobSharedSecret, carolSharedSecret))
            throw new Exception("Bob and Carol differ");
        System.out.println("Bob and Carol are the same");
    // Compare Carol and Sara
        if (!java.util.Arrays.equals(carolSharedSecret, saraSharedSecret))
            throw new Exception("Carol and Sara differ");