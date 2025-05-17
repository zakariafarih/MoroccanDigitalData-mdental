package org.mdental.commons;

import org.junit.jupiter.api.Test;
import org.mdental.commons.model.ApiResponse;
import org.mdental.commons.model.ErrorCode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class CommonsApplicationTests {

	@Test
	void testApiResponseSuccess() {
		String testData = "test data";
		ApiResponse<String> response = ApiResponse.success(testData);

		assertTrue(response.isSuccess());
		assertEquals(testData, response.getData());
	}

	@Test
	void testApiResponseError() {
		ErrorCode errorCode = ErrorCode.GENERAL_ERROR;
		String errorMessage = "Test error";

		ApiResponse<Object> response = ApiResponse.error(errorCode, errorMessage);

		assertFalse(response.isSuccess());
		assertEquals(errorCode, response.getError().getCode());
		assertEquals(errorMessage, response.getError().getMessage());
	}
}