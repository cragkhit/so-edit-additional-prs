        assertThrows(new Runnable() {
          @Override public void run() { methodThatThrows(); }
        });
    ...in Java 6/7. Importantly, _assertThrows is called before methodThatThrows_, so it can invoke methodThatThrows. Thanks Stefan for [pointing out Fishbowl][fishbowl], but you could easily write an equivalent yourself:
        public void assertThrows(Runnable block) {
          try {
            block.run();
            fail("Block didn't throw.");
          } catch (Exception ex) { }
        }