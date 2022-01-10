package com.intellij.tasks.impl.gson;

import java.util.Date;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.intellij.tasks.impl.TaskUtil;

/**
 * @author Mikhail Golubev
 */
public class TaskGsonUtil
{

	private TaskGsonUtil()
	{
	}

	public static final JsonDeserializer<Date> DATE_DESERIALIZER = (json, typeOfT, context) -> TaskUtil.parseDate(json.getAsString());

	/**
	 * Create default GsonBuilder used to create Gson factories across various task repository implementations.
	 * It uses {@link TaskUtil#formatDate(Date)} to parse dates and {@link NullCheckingFactory}
	 * to preserve null-safety of objects returned by server.
	 *
	 * @return described builder
	 * @see NullCheckingFactory
	 * @see Mandatory
	 * @see RestModel
	 */
	public static GsonBuilder createDefaultBuilder()
	{
		return new GsonBuilder().registerTypeAdapter(Date.class, DATE_DESERIALIZER).registerTypeAdapterFactory(NullCheckingFactory.INSTANCE);
	}
}
