package presentacio.listener;

@FunctionalInterface
public interface OnLlistaMembershipChanged {
    void onMembershipChanged(long isbn, int llistaId, boolean added);
}
