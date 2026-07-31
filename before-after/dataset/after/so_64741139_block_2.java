    public static void main(String[] args) throws Exception {
        // Generate public and private keys using RSA
        Map<String, Object> keys = getRSAKeys();
    
        PrivateKey privateKey = (PrivateKey) keys.get("private");
        PublicKey publicKey = (PublicKey) keys.get("public");
    
        StringBuilder keypublic = new StringBuilder();
    
        keypublic.append("-----BEGIN PUBLIC KEY-----\n");
        keypublic.append(Base64.getMimeEncoder().encodeToString(publicKey.getEncoded()) + "\n");
        keypublic.append("-----END PUBLIC KEY-----\n");
    
    
        String keyEncodedPublic = Base64.getEncoder().encodeToString(keypublic.toString().getBytes());
    
        String signature = sign("MyEncryptedInternalString", privateKey);
        System.out.println("key: \n" + Base64.getEncoder().encodeToString(signature.getBytes()) + ":" + keyEncodedPublic);
    
    
        System.out.println("\n");
        
        while(true) {
    
    
            Scanner out = new Scanner(System.in);
            System.out.print("Insert text: ");
            String enc = out.nextLine();
    
    
            String descryptedText = decryptMessage(enc, privateKey);
            System.out.println("Decrypted: " + descryptedText);
            System.out.println();
    
            Scanner out2 = new Scanner(System.in);
            System.out.print("Insert text2: ");
            String enc2 = out2.nextLine();
    
            byte[] decodedBytesKey = Base64.getDecoder().decode(enc2);
    
            //String content = encryptMessage("message from the client", new String(decodedBytes));
    
            String publicKeyPEM = new String(decodedBytesKey)
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replaceAll(System.lineSeparator(), "")
                    .replace("-----END PUBLIC KEY-----", "");
    
            byte[] encoded = Base64.getDecoder().decode(publicKeyPEM);
    
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
            PublicKey p = keyFactory.generatePublic(keySpec);
    
            System.out.println(encryptMessage("Message from client",p));
    
        }
    
    
    }
    
    
    public static boolean verify(String plainText, String signature, PublicKey publicKey) throws Exception {
        Signature publicSignature = Signature.getInstance("SHA256withRSA");
        publicSignature.initVerify(publicKey);
        publicSignature.update(plainText.getBytes(StandardCharsets.UTF_8));
    
        byte[] signatureBytes = Base64.getDecoder().decode(signature);
    
        return publicSignature.verify(signatureBytes);
    }
    
    public static String sign(String plainText, PrivateKey privateKey) throws Exception {
        Signature privateSignature = Signature.getInstance("SHA256withRSA");
        privateSignature.initSign(privateKey);
        privateSignature.update(plainText.getBytes(StandardCharsets.UTF_8));
    
        byte[] signature = privateSignature.sign();
    
        return Base64.getEncoder().encodeToString(signature);
    }
    
    
    // Get RSA keys. Uses key size of 2048.
    private static Map<String,Object> getRSAKeys() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        PrivateKey privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();
    
        Map<String, Object> keys = new HashMap<String,Object>();
        keys.put("private", privateKey);
        keys.put("public", publicKey);
        return keys;
    }
    
    
    private static String decryptMessage(String encryptedText, PrivateKey privateKey) throws Exception {
        Cipher cipher =  Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        return new String(cipher.doFinal(Base64.getDecoder().decode(encryptedText)));
    }
    
    
    
    private static String encryptMessage(String plainText, PublicKey publicKey) throws Exception {
        Cipher cipher =  Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return URLEncoder.encode(Base64.getEncoder().encodeToString(cipher.doFinal(plainText.getBytes())));
    }