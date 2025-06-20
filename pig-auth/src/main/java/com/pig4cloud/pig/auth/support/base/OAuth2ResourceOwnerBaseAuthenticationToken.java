package com.pig4cloud.pig.auth.support.base;

import lombok.Getter;
import org.springframework.lang.Nullable;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.util.Assert;

import java.util.*;

/**
 * @author lengleng
 * @date 2022/6/2
 *
 * 自定义授权模式抽象
 */
//o2 用户自定义的token，只是为了接收前端传的请求，封装的一个token，不是最终认证完成的 token
//o2 2.7 自定义token，变量 授权类型， 客户端认证， scopes， 额外的参数，比如客户端id 秘钥， 手机号 验证码， 用户 密码，
//o2 2.8 需要思考的是这些参数都在哪用到了，删掉行不行，对应什么功能
public abstract class OAuth2ResourceOwnerBaseAuthenticationToken extends AbstractAuthenticationToken {

	@Getter
	private final AuthorizationGrantType authorizationGrantType;

	@Getter
	private final Authentication clientPrincipal;

	@Getter
	private final Set<String> scopes;

	@Getter
	private final Map<String, Object> additionalParameters;

	public OAuth2ResourceOwnerBaseAuthenticationToken(AuthorizationGrantType authorizationGrantType,
			Authentication clientPrincipal, @Nullable Set<String> scopes,
			@Nullable Map<String, Object> additionalParameters) {
		//o2 2.9 标识用户token的认证状态为false
		super(Collections.emptyList());
		Assert.notNull(authorizationGrantType, "authorizationGrantType cannot be null");
		Assert.notNull(clientPrincipal, "clientPrincipal cannot be null");
		this.authorizationGrantType = authorizationGrantType;
		this.clientPrincipal = clientPrincipal;
		this.scopes = Collections.unmodifiableSet(scopes != null ? new HashSet<>(scopes) : Collections.emptySet());
		this.additionalParameters = Collections.unmodifiableMap(
				additionalParameters != null ? new HashMap<>(additionalParameters) : Collections.emptyMap());
	}

	/**
	 * 扩展模式一般不需要密码
	 */
	@Override
	public Object getCredentials() {
		return "";
	}

	/**
	 * 获取用户名
	 */
	@Override
	public Object getPrincipal() {
		return this.clientPrincipal;
	}

}
