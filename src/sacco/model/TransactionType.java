package sacco.model;

/**
 * Topic 6 - Enumerated Types, with per-constant data and behaviour.
 */
public enum TransactionType {

    DEPOSIT("DEP", +1),
    WITHDRAWAL("WDL", -1),
    LOAN_DISBURSEMENT("DSB", +1),
    LOAN_REPAYMENT("RPY", -1),
    INTEREST_CREDIT("INT", +1),
    INTEREST_CHARGE("CHG", -1);

    private final String code;

    /** +1 adds to the balance, -1 subtracts. */
    private final int sign;

    private TransactionType(String code, int sign) {
        this.code = code;
        this.sign = sign;
    }

    public String getCode() {
        return code;
    }

    public int getSign() {
        return sign;
    }

    public boolean isCredit() {
        return sign > 0;
    }

    /**
     * Enum constants can be looked up from text.
     * valueOf() throws IllegalArgumentException for unknown names, so we guard it.
     */
    public static TransactionType fromCode(String code) {
        // values() returns an array of every constant - Topic 4 (Arrays, Enhanced for)
        for (TransactionType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown transaction code: " + code);
    }
}
