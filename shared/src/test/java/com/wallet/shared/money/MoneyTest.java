package com.wallet.shared.money;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    private static Money money(String amount, String currency) {
        return new Money(new BigDecimal(amount), currency);
    }

    @Nested
    @DisplayName("Validación")
    class Validacion {

        @Test
        @DisplayName("Acepta amount positivo y currency ISO-4217")
        void acepta_amountPositivo_currencyIso4217() {
            Money m = money("100.50", "USD");

            assertThat(m.amount()).isEqualByComparingTo("100.50");
            assertThat(m.currency()).isEqualTo("USD");
        }

        @Test
        @DisplayName("Rechaza amount cero")
        void rechaza_amountCero() {
            assertThatThrownBy(() -> money("0", "USD"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("amount must be > 0");
        }

        @Test
        @DisplayName("Rechaza amount negativo")
        void rechaza_amountNegativo() {
            assertThatThrownBy(() -> money("-1", "USD"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("amount must be > 0");
        }

        @Test
        @DisplayName("Rechaza amount nulo")
        void rechaza_amountNulo() {
            assertThatThrownBy(() -> new Money(null, "USD"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("amount");
        }

        @Test
        @DisplayName("Rechaza currency nula")
        void rechaza_currencyNula() {
            assertThatThrownBy(() -> money("10", null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("currency");
        }

        @Test
        @DisplayName("Acepta currencies ISO-4217 distintas (USD, EUR, JPY, GBP)")
        void acepta_currenciesIso4217() {
            for (String code : new String[] { "USD", "EUR", "JPY", "GBP" }) {
                assertThat(Currency.getInstance(code)).isNotNull();
                assertThat(Money.isIso4217(code)).isTrue();
            }
        }

        @Test
        @DisplayName("Rechaza currency en minúsculas")
        void rechaza_currencyMinusculas() {
            assertThat(Money.isIso4217("usd")).isFalse();
            assertThatThrownBy(() -> money("10", "usd"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ISO-4217");
        }

        @Test
        @DisplayName("Rechaza currency de longitud distinta a 3")
        void rechaza_currencyLongitudInvalida() {
            assertThat(Money.isIso4217("US")).isFalse();
            assertThat(Money.isIso4217("USDX")).isFalse();
            assertThat(Money.isIso4217("")).isFalse();
            assertThat(Money.isIso4217(null)).isFalse();
        }

        @Test
        @DisplayName("Rechaza currency con caracteres no letras")
        void rechaza_currencyConCaracteresInvalidos() {
            assertThat(Money.isIso4217("US1")).isFalse();
            assertThat(Money.isIso4217("U-D")).isFalse();
            assertThat(Money.isIso4217("U5D")).isFalse();
        }
    }

    @Nested
    @DisplayName("Aritmética")
    class Aritmetica {

        @Test
        @DisplayName("Suma misma moneda retorna nueva instancia con importe sumado")
        void add_mismaMoneda_retornaNuevaInstancia() {
            Money a = money("100.00", "USD");
            Money b = money("50.25", "USD");

            Money result = a.add(b);

            assertThat(result.amount()).isEqualByComparingTo("150.25");
            assertThat(result.currency()).isEqualTo("USD");
            assertThat(result).isNotSameAs(a);
            assertThat(result).isNotSameAs(b);
            assertThat(a.amount()).isEqualByComparingTo("100.00");
        }

        @Test
        @DisplayName("Suma distinta moneda lanza excepción")
        void add_distintaMoneda_lanzaExcepcion() {
            Money a = money("100.00", "USD");
            Money b = money("50.00", "EUR");

            assertThatThrownBy(() -> a.add(b))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("currency mismatch");
        }

        @Test
        @DisplayName("Resta misma moneda retorna nueva instancia con importe restado")
        void subtract_mismaMoneda_retornaNuevaInstancia() {
            Money a = money("100.00", "USD");
            Money b = money("30.00", "USD");

            Money result = a.subtract(b);

            assertThat(result.amount()).isEqualByComparingTo("70.00");
            assertThat(result.currency()).isEqualTo("USD");
            assertThat(result).isNotSameAs(a);
            assertThat(a.amount()).isEqualByComparingTo("100.00");
        }

        @Test
        @DisplayName("Resta con resultado cero lanza excepción (Money rechaza amount=0)")
        void subtract_resultadoCero_lanzaExcepcion() {
            Money a = money("100.00", "USD");
            Money b = money("100.00", "USD");

            assertThatThrownBy(() -> a.subtract(b))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("amount must be > 0");
        }

        @Test
        @DisplayName("Resta con resultado negativo lanza excepción")
        void subtract_resultadoNegativo_lanzaExcepcion() {
            Money a = money("100.00", "USD");
            Money b = money("100.01", "USD");

            assertThatThrownBy(() -> a.subtract(b))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("negative");
        }

        @Test
        @DisplayName("Resta distinta moneda lanza excepción")
        void subtract_distintaMoneda_lanzaExcepcion() {
            Money a = money("100.00", "USD");
            Money b = money("30.00", "EUR");

            assertThatThrownBy(() -> a.subtract(b))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("currency mismatch");
        }

        @Test
        @DisplayName("isGreaterThanOrEqual retorna true cuando balance ≥ amount")
        void isGreaterThanOrEqual_true() {
            Money balance = money("100.00", "USD");
            Money amount = money("100.00", "USD");

            assertThat(balance.isGreaterThanOrEqual(amount)).isTrue();
        }

        @Test
        @DisplayName("isGreaterThanOrEqual retorna false cuando balance < amount")
        void isGreaterThanOrEqual_false() {
            Money balance = money("99.99", "USD");
            Money amount = money("100.00", "USD");

            assertThat(balance.isGreaterThanOrEqual(amount)).isFalse();
        }

        @Test
        @DisplayName("isGreaterThanOrEqual con distinta moneda lanza excepción")
        void isGreaterThanOrEqual_distintaMoneda_lanzaExcepcion() {
            Money usd = money("100.00", "USD");
            Money eur = money("100.00", "EUR");

            assertThatThrownBy(() -> usd.isGreaterThanOrEqual(eur))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Igualdad")
    class Igualdad {

        @Test
        @DisplayName("Mismo valor retorna true")
        void mismoValor_true() {
            Money a = money("100.00", "USD");
            Money b = money("100.00", "USD");

            assertThat(a).isEqualTo(b);
            assertThat(a.hashCode()).isEqualTo(b.hashCode());
        }

        @Test
        @DisplayName("Distinto currency retorna false")
        void distintoCurrency_false() {
            Money usd = money("100.00", "USD");
            Money eur = money("100.00", "EUR");

            assertThat(usd).isNotEqualTo(eur);
        }

        @Test
        @DisplayName("Distinto amount retorna false")
        void distintoAmount_false() {
            Money a = money("100.00", "USD");
            Money b = money("100.01", "USD");

            assertThat(a).isNotEqualTo(b);
        }

        @Test
        @DisplayName("Comparado con null retorna false")
        void contraNull_false() {
            Money a = money("100.00", "USD");

            assertThat(a).isNotEqualTo(null);
        }

        @Test
        @DisplayName("Comparado con otro tipo retorna false")
        void contraOtroTipo_false() {
            Money a = money("100.00", "USD");

            assertThat(a).isNotEqualTo("100.00 USD");
        }

        @Test
        @DisplayName("JSON: serializa como \"<amount> <currency>\"")
        void json_serializaComoAmountCurrency() {
            Money m = money("100.50", "USD");

            assertThat(m.json()).isEqualTo("100.50 USD");
        }

        @Test
        @DisplayName("JSON: parse recupera la instancia con los mismos valores")
        void json_parse_roundTrip() {
            Money original = money("123.45", "EUR");

            Money parsed = Money.parse(original.json());

            assertThat(parsed).isEqualTo(original);
        }

        @Test
        @DisplayName("JSON: parse rechaza entrada malformada")
        void json_parse_rechazaMalformado() {
            assertThatThrownBy(() -> Money.parse(null))
                .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> Money.parse(""))
                .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> Money.parse("100"))
                .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> Money.parse("100 USD EUR"))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
