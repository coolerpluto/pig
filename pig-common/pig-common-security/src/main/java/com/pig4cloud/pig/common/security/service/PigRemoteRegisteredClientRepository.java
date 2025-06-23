package com.pig4cloud.pig.common.security.service;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import com.pig4cloud.pig.admin.api.entity.SysOauthClientDetails;
import com.pig4cloud.pig.admin.api.feign.RemoteClientDetailsService;
import com.pig4cloud.pig.common.core.constant.CacheConstants;
import com.pig4cloud.pig.common.core.constant.SecurityConstants;
import com.pig4cloud.pig.common.core.util.RetOps;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationException;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

/**
 * 查询客户端相关信息实现
 *
 * @author lengleng
 * @date 2022/5/29
 */
//o2? 这里为什么会注册为bean
@RequiredArgsConstructor
public class PigRemoteRegisteredClientRepository implements RegisteredClientRepository {

	/**
	 * 刷新令牌有效期默认 30 天
	 */
	private final static int refreshTokenValiditySeconds = 60 * 60 * 24 * 30;

	/**
	 * 请求令牌有效期默认 12 小时
	 */
	private final static int accessTokenValiditySeconds = 60 * 60 * 12;

	private final RemoteClientDetailsService clientDetailsService;

	/**
	 * Saves the registered client.
	 *
	 * <p>
	 * IMPORTANT: Sensitive information should be encoded externally from the
	 * implementation, e.g. {@link RegisteredClient#getClientSecret()}
	 * @param registeredClient the {@link RegisteredClient}
	 */
	@Override
	public void save(RegisteredClient registeredClient) {
	}

	/**
	 * Returns the registered client identified by the provided {@code id}, or
	 * {@code null} if not found.
	 * @param id the registration identifier
	 * @return the {@link RegisteredClient} if found, otherwise {@code null}
	 */
	@Override
	public RegisteredClient findById(String id) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Returns the registered client identified by the provided {@code clientId}, or
	 * {@code null} if not found.
	 * @param clientId the client identifier
	 * @return the {@link RegisteredClient} if found, otherwise {@code null}
	 */

	/**
	 * 重写原生方法支持redis缓存
	 * @param clientId
	 * @return
	 */
	//? Cacheable怎么用
	@Override
	@SneakyThrows
	@Cacheable(value = CacheConstants.CLIENT_DETAILS_KEY, key = "#clientId", unless = "#result == null")
	public RegisteredClient findByClientId(String clientId) {

		//o2? 这个retops解决了什么
		SysOauthClientDetails clientDetails = RetOps.of(clientDetailsService.getClientDetailsById(clientId))
			.getData()
			.orElseThrow(() -> new OAuth2AuthorizationCodeRequestAuthenticationException(
					new OAuth2Error("客户端查询异常，请检查数据库链接"), null));

		//o2 3.10 这个实现类PigRemoteRegisteredClientRepository实现了RegisteredClientRepository接口，这个接口就是解决oauth2里面
		// 对客户端信息的存储和查询的，用于客户端认证的信息比对
		//o2 3.11 配置一些客户端的数据
		RegisteredClient.Builder builder = RegisteredClient.withId(clientDetails.getClientId())
			.clientId(clientDetails.getClientId())
			.clientSecret(SecurityConstants.NOOP + clientDetails.getClientSecret())
				//o2 .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
				// 表示客户端必须通过 HTTP Basic Auth 方式传递 client_id 和 client_secret 来进行认证。
				//  其中 Header 带有：Basic Y2xpZW50SWQ6Y2xpZW50U2VjcmV0
			.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC);

		//o2 3.12 配置支持客户端的grant_type
		for (String authorizedGrantType : clientDetails.getAuthorizedGrantTypes()) {
			builder.authorizationGrantType(new AuthorizationGrantType(authorizedGrantType));

		}

		//o2 3.13 这里配置授权服务器存储的scopes和回调地址，解决了前面的疑问，为什么数据库存的是逗号拼接的，而provider里debugger到的scope或者
		// redirectUri是一个数组
		// 回调地址
		Optional.ofNullable(clientDetails.getWebServerRedirectUri())
			.ifPresent(redirectUri -> Arrays.stream(redirectUri.split(StrUtil.COMMA))
				.filter(StrUtil::isNotBlank)
					//o2 3.13.1 调用builder的redirectUri，将每个scope添加进去
				.forEach(builder::redirectUri));

		// scope
		Optional.ofNullable(clientDetails.getScope())
			.ifPresent(scope -> Arrays.stream(scope.split(StrUtil.COMMA))
				.filter(StrUtil::isNotBlank)
					//o2 3.13.2 调用builder的scope方法，将每个scope添加进去
				.forEach(builder::scope));

		return builder
				//o2 3.14 配置token的设置相关，
			.tokenSettings(TokenSettings.builder()
					//o2 3.14.1 配置token的格式，reference代表存储型token，是一个uuid，存储在redis或数据库中
					//o2 还有一种SELF_CONTAINED 是jwt格式，非存储型，直接从jwt解密
				.accessTokenFormat(OAuth2TokenFormat.REFERENCE)
					//o2 3.14.2 设置accestoken的有效期
				.accessTokenTimeToLive(Duration.ofSeconds(
						Optional.ofNullable(clientDetails.getAccessTokenValidity()).orElse(accessTokenValiditySeconds)))
					//o2 3.14.3 设置refreshtoken的有效期
				.refreshTokenTimeToLive(Duration.ofSeconds(Optional.ofNullable(clientDetails.getRefreshTokenValidity())
					.orElse(refreshTokenValiditySeconds)))
				.build())
				//o2 3.15 配置客户端的授权访问页是否跳过
			.clientSettings(ClientSettings.builder()
				.requireAuthorizationConsent(!BooleanUtil.toBoolean(clientDetails.getAutoapprove()))
				.build())
			.build();

	}

}
