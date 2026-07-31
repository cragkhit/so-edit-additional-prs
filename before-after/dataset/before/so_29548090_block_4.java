    public class ImmutableMap<K, V> implements Map<K, V> {
    	private HashMap<K, V> map;
    	
    	public ImmutableMap(HashMap<K, V> map) {
    		this.map = map;
    	}
    
    	@Override
    	public int size() {
    		return map.size();
    	}
    
    	@Override
    	public boolean isEmpty() {
    		return map.isEmpty();
    	}
    
    	@Override
    	public boolean containsKey(Object key) {
    		return map.containsKey(key);
    	}
    
    	@Override
    	public boolean containsValue(Object value) {
    		return map.containsValue(value);
    	}
    
    	@Override
    	public V get(Object key) {
    		return map.get(key);
    	}
    
    	@Override
    	public V put(K key, V value) {
    		if(!map.containsKey(key)) {
    			throw new IllegalArgumentException("Cannot add new keys!");
    		}
    		
    		return map.put(key, value);
    	}
    
    	@Override
    	public V remove(Object key) {
    		throw new IllegalStateException("You cannot remove entries from this map!");
    	}
    
    	@Override
    	public void putAll(Map<? extends K, ? extends V> map) {
    		for(K key : map.keySet()) {
    			if(!this.map.containsKey(key)) {
    				throw new IllegalArgumentException("Cannot add entries from a map without similar keys!");
    			}
    		}
    		
    		this.map.putAll(map);
    	}
    
    	@Override
    	public void clear() {
    		throw new IllegalStateException("You cannot remove entries from this map!");
    	}
    
    	@Override
    	public Set<K> keySet() {
    		return new HashSet<>(map.keySet());
    	}
    
    	@Override
    	public Collection<V> values() {
    		return map.values();
    	}
    
    	@Override
    	public Set<Map.Entry<K, V>> entrySet() {
            //to allow modification of values, create your own ("immutable") entry set and return that
    		return new HashSet<>(map.entrySet()); 
    	}
    }