    Document doc = 
            XPathFactory xPathfactory = XPathFactory.newInstance();
            XPath xpath = xPathfactory.newXPath();
            XPathExpression expr = xpath.compile("//test//elem");
            NodeList all = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
            Set<String> values = new HashSet<>();
            if (all != null || all.getLength() > 0) {
                for (int i = 0; i < all.getLength(); i++) {
                    values.add(all.item(i).getTextContent());
                }
            }