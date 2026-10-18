/* Build-time configuration.
 *
 * bgmProxy stays empty in the committed source on purpose. A personal build injects its
 * own Cloudflare proxy URL into the packaged copy; the public build falls back to the
 * official api.bgm.tv endpoint and uses none of the maintainer's infrastructure.
 */
window.FOLIOFRAME_CONFIG = { bgmProxy: '' };
