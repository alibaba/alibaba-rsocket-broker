package com.alibaba.rsocket.observability;

/**
 * Rsocket broker error info context:
 * [RST-200000] SUCCESS
 * [RST-1xxxxx] {@code parameter} error. Define parameter check exception
 * [RST-2xxxxx] {@code business} error. Define business logic exception
 * [RST-3xxxxx] {@code repository service}. Define repository operation exception
 * [RST-4xxxxx] {@code dependent service}. Define dependency service exception
 * [RST-5xxxxx] {@code system} error. Define application system exception
 */
public enum RsocketErrorCodes implements RsocketErrorCodeContext {

    /**
     * The successful error code.
     */
    SUCCESS("200000", "Success"),

    /*-------------------------------------------Parameter error as below---------------------------------------**/
    /**
     * Invalid basic parameter error, the code starts with 0.
     */
    PARAMETER_ERROR(
            GENERATOR.createParameterErrorCode("00000"), "Invalid parameter error!"),

    /*-------------------------------------------Business error as below---------------------------------------**/
    /**
     * Basic business error, the code starts with 0.
     */
    BUSINESS_ERROR(
            GENERATOR.createBusinessProcessErrorCode("00001"), "Business error!"),

    /*-------------------------------------------Repository error as below---------------------------------------**/
    /**
     * Basic repository error, the code starts with 0.
     */
    REPOSITORY_ERROR(
            GENERATOR.createRepositoryErrorCode("00000"), "Repository error!"),

    /*-------------------------------------------Dependent service error as below---------------------------------------**/
    /**
     * Basic dependent service error, the code starts with 0.
     */
    DEPENDENT_SERVICE_ERROR(
            GENERATOR.createDependentServiceErrorCode("00000"), "Failed to call the dependent service!"),

    /*-------------------------------------------System error as below---------------------------------------**/
    /**
     * Basic system error, the code starts with 0.
     */
    SYSTEM_ERROR(
            GENERATOR.createSystemErrorCode("00000"), "System error!"),
    ;

    // -- Encapsulation

    private final String code;
    private final String message;

    RsocketErrorCodes(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String getCode() {
        return this.code;
    }

    @Override
    public String getMessage() {
        return this.message;
    }

    static {
        ErrorCodeManager.assertErrorCodesNoDuplicate(RsocketErrorCodes.values());
    }

    /**
     * Show error codes.
     */
    public static void showErrorCodes() {
        MANAGER.showErrorCodes(RsocketErrorCodes.values());
    }
}
