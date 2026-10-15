package az.yusif.fx.exception;

import java.time.LocalDate;

public class RatesNotPublishedException extends NotFoundException {

    public RatesNotPublishedException(LocalDate date) {
        super("CBAR has not published rates for " + date);
    }
}
