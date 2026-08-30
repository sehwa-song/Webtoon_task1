package task1.chain;

/**
 * Chain of Responsibility의 공통 Handler입니다.
 * 현재 검사를 통과한 후보만 다음 Handler로 전달합니다.
 */
public abstract class BlankZoneHandler {
    private BlankZoneHandler next;

    /** 다음 책임 객체를 연결하고 그 객체를 반환합니다. */
    public final BlankZoneHandler setNext(BlankZoneHandler nextHandler) {
        this.next = nextHandler;
        return nextHandler;
    }

    /** 현재 책임이 거부하면 즉시 종료하고, 통과하면 다음으로 전달합니다. */
    public final boolean handle(BlankZoneContext context) {
        if (!check(context)) {
            return false;
        }
        return next == null || next.handle(context);
    }

    protected abstract boolean check(BlankZoneContext context);
}
