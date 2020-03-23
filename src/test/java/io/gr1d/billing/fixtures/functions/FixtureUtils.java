package io.gr1d.billing.fixtures.functions;

import br.com.six2six.fixturefactory.function.AtomicFunction;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoField;
import java.util.Date;
import java.util.Random;

public class FixtureUtils {
	public static String date(final String from, final String to, final String format) {
		try {
			final SimpleDateFormat sdf = new SimpleDateFormat(format);
			final long start = sdf.parse(from).getTime();
			final long end = sdf.parse(to).getTime();
			final long diff = end - start + 1;
			final long rand = Math.abs(new Random().nextLong()) % diff;
			
			return sdf.format(new Date(start + rand));
		} catch (final ParseException e) {
			return null;
		}
	}
	
	public static String number(final int digits) {
		final StringBuilder str = new StringBuilder();
		final Random rand = new Random();
		
		for (int i = 0; i < digits; i++) {
			str.append(rand.nextInt(10));
		}
		
		return str.toString();
	}
	
	public static final String json(final Object fixtureObject) {
		final ObjectMapper objMapper = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL)
				.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
				.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		
		return json(fixtureObject, objMapper);
	}
	
	public static final String json(final Object fixtureObject, final ObjectMapper objMapper) {
		try {
			return objMapper.writeValueAsString(fixtureObject);
		} catch (final JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}
}
