/*
✨ CoonDev • https://dev.coonlink.com/

 ▄█▄    ████▄ ████▄    ▄   ██▄   ▄███▄      ▄
 █▀ ▀▄  █   █ █   █     █  █  █  █▀   ▀      █
 █   ▀  █   █ █   █ ██   █ █   █ ██▄▄   █     █
 █▄  ▄▀ ▀████ ▀████ █ █  █ █  █  █▄   ▄▀ █    █
 ▀███▀              █  █ █ ███▀  ▀███▀    █  █
                    █   ██                 █▐
                                           ▐
*/

package xyz.gramapple.wallet;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

final class Format {
    private Format() {}

    static String shortGram(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {return "—";}
        if (value < 0) {return "-" + shortGram(-value);}
        return cardNumber(value);
    }

    static String chunkAddress(String address) {
        if (address == null || address.isEmpty()) {return "";}
        String raw = address.replaceAll("\\s+", "");
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < raw.length(); i += 4) {
            if (out.length() > 0) {out.append(i % 16 == 0 ? '\n' : ' ');}
            out.append(raw, i, Math.min(i + 4, raw.length()));
        }
        return out.toString();
    }

    static CharSequence chunkAddressStyled(String address) {
        if (address == null || address.isEmpty()) {return "";}
        String raw = address.replaceAll("\\s+", "");
        SpannableStringBuilder out = new SpannableStringBuilder();
        int chunk = 0;
        for (int i = 0; i < raw.length(); i += 4) {
            if (out.length() > 0) {out.append(i % 16 == 0 ? '\n' : ' ');}
            int start = out.length();
            out.append(raw, i, Math.min(i + 4, raw.length()));
            if ((chunk & 1) == 1) {out.setSpan(new ForegroundColorSpan(0xFF707579), start, out.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);}
            chunk++;
        }
        return out;
    }

    static String usd(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value) || value <= 0) {return "—";}
        return cardNumber(value) + " $";
    }

    static String tokenAmount(String raw, int decimals) {
        try {
            BigDecimal value = new BigDecimal(raw == null || raw.isEmpty() ? "0" : raw).movePointLeft(Math.max(0, decimals));
            value = value.stripTrailingZeros();
            if (value.scale() < 0) {value = value.setScale(0);}
            if (value.scale() > 8) {value = value.setScale(8, RoundingMode.DOWN).stripTrailingZeros();}
            DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
            symbols.setGroupingSeparator(' ');
            symbols.setDecimalSeparator(',');
            DecimalFormat format = new DecimalFormat("#,##0.########", symbols);
            format.setGroupingUsed(true);
            format.setGroupingSize(3);
            return format.format(value);
        } catch (Exception error) {return "—";}
    }

    static String tokenUsd(String raw, int decimals, Double price) {
        if (price == null || price <= 0) {return "";}
        try {
            BigDecimal amount = new BigDecimal(raw == null || raw.isEmpty() ? "0" : raw).movePointLeft(Math.max(0, decimals));
            double usd = amount.multiply(BigDecimal.valueOf(price)).doubleValue();
            return usd(usd);
        } catch (Exception error) {return "";}
    }

    private static String cardNumber(double value) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator(' ');
        symbols.setDecimalSeparator(',');
        DecimalFormat format = new DecimalFormat("#,##0.00", symbols);
        format.setGroupingUsed(true);
        format.setGroupingSize(3);
        return format.format(value);
    }
}
