package io.gr1d.billing.response;

public enum CardBrand {

    UNKNOWN(".*"),
    AMERICAN_EXPRESS("3[47]\\d+"),
    CHINA_UNIONPAY("(62|88)\\d+"),
    DINERS_CLUB("3([689]|0[90-5])\\d+"),
    DISCOVER("6(011|4[4-9]|5)\\d+"),
    JCB("(352[89]|35[3-8][0-9])\\d+"),
    LASER("(6304|6706|6771|6709)\\d+"),
    MAESTRO("(5018|5020|5038|5612|5893|6304|6759|6761|6762|6763|0604|6390)\\d+"),
    DANKORT("5019\\d+"),
    MASTERCARD("5[0-5]\\d+"),
    VISA_ELECTRON("(4026|417500|4405|4508|4844|4913|4917)\\d+"),
    VISA("4\\d+");

    private String pattern;

    CardBrand(final String pattern) {
        this.pattern = pattern;
    }

    private boolean validate(final String cardPrefix) {
        return cardPrefix != null && cardPrefix.matches(pattern);
    }

    /**
     * Returns the brand for a given card prefix (the first 5 digits are enough,
     * and PCI-compliant)
     *
     * @param cardPrefix The 5 first digits of the card number
     * @return The card brand, or {@link #UNKNOWN}.
     */
    public static CardBrand of(final String cardPrefix) {
        for (final CardBrand brand : values()) {
            if (brand != UNKNOWN && brand.validate(cardPrefix)) {
                return brand;
            }
        }

        return UNKNOWN;
    }

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}

