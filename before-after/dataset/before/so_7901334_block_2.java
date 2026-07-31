public class CopyOnWriteWeakReferenceSet&lt;E> extends AbstractSet&lt;E> {
	private static final long serialVersionUID = -3127064371410565215L;
	private CopyOnWriteArraySet&lt;WeakReference&lt;E>> items;
	public CopyOnWriteWeakReferenceSet() {
		items = new CopyOnWriteArraySet&lt;WeakReference&lt;E>>();
	}
	public CopyOnWriteWeakReferenceSet(Collection&lt;E> c) {
		items = new CopyOnWriteArraySet&lt;WeakReference&lt;E>>();
		addAll(c);
	}
	public boolean add(E element) {
		if (contains(element)) return false;
		return items.add(new WeakReference&lt;E>(element));
	}
	@Override
	public boolean remove(Object o) {
		boolean removed = false;
		E element = null;
		for (WeakReference&lt;E> ref : items) {
			element = ref.get();
			if (element != null && element.equals(o)) {
				ref.clear();
				removed = true;
				break;
			}
		}
		return removed;
	}
	@Override
	public boolean contains(Object o) {
		List&lt;E> list = new ArrayList&lt;E>();
		for (WeakReference&lt;E> ref : items) {
			if (ref.get() != null) {
				list.add(ref.get());
			}
		}
		boolean contains = list.contains(o);
		list.clear();
		list = null;
		return contains;
	}
	public int size() {
		removeReleased();
		return items.size();
	}
	private void removeReleased() {
		for (WeakReference&lt;E> ref : items) {
			if (ref.get() == null) {
				items.remove(ref);
			}
		}
	}
	@Override
	public Iterator&lt;E> iterator() {
		final Iterator&lt;WeakReference&lt;E>> iter = items.iterator();
		return new Iterator&lt;E>() {
			@Override
			public boolean hasNext() {
				return iter.hasNext();
			}
			@Override
			public E next() {
				return iter.next().get();
			}
			@Override
			public void remove() {
				iter.remove();
			}
		};
	}
	public static void main(String[] args) {
		CopyOnWriteWeakReferenceSet&lt;String> set = 
                      new CopyOnWriteWeakReferenceSet&lt;String>();
		set.add("1");
		set.add("2");
		set.add("1"); //returns false
		
		System.out.println("Size of set " + set.size()); //prints 1
		//Testing the iterator.
		for (String str : set) {
			System.out.println(str);
		}
	}
}