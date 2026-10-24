<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>

<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">
    <meta name="color-scheme" content="light">
    <script>document.documentElement.classList.add('js');</script>
    <title>TicketHub – Nền tảng bán vé sự kiện thông minh</title>
    <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Be+Vietnam+Pro:wght@500;600;700;800;900&display=swap">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/style.css">
</head>
<body>

<!-- NAVIGATION BAR CHUẨN VỚI LOGO Ở GÓC TRÊN BÊN TRÁI -->
<nav>
    <div class="w">
        <!-- LOGO GÓC TRÊN BÊN TRÁI (THEO ĐÚNG HÌNH ẢNH LOGO) -->
        <a href="${pageContext.request.contextPath}/home" class="logo-container" title="TicketHub">
            <div class="logo-badge">
                <img src="${pageContext.request.contextPath}/assets/images/logo.svg" alt="TicketHub Logo">
            </div>
            <div class="logo-pill">TICKETHUB</div>
        </a>

        <!-- CÁC LIÊN KẾT ĐIỀU HƯỚNG -->
        <a class="l active" href="${pageContext.request.contextPath}/home">Trang chủ</a>
        <a class="l" href="#ev">Sự kiện</a>
        <%-- CUSTOMER (và khách chưa đăng nhập) thấy "Vé của tôi"; chỉ ADMIN thấy Dashboard --%>
        <c:if test="${sessionScope.currentUser.role != 'ADMIN'}">
            <a class="l" href="${pageContext.request.contextPath}/orders">Vé của tôi</a>
        </c:if>
        <c:if test="${sessionScope.currentUser.role == 'ADMIN'}">
            <a class="l dash-link" href="${pageContext.request.contextPath}/dashboard">📊 Dashboard</a>
        </c:if>

        <div class="sp"></div>

        <!-- Ô TÌM KIẾM NHANH SỰ KIỆN -->
        <form class="sb" action="${pageContext.request.contextPath}/home" method="get">
            <input id="q" name="q" placeholder="Tìm sự kiện" aria-label="Tìm sự kiện" value="${keyword}">
            <button type="submit" aria-label="Tìm">🔍</button>
        </form>

        <!-- KHỐI TÀI KHOẢN / ĐĂNG NHẬP / ĐĂNG KÝ -->
        <c:choose>
            <c:when test="${not empty sessionScope.currentUser}">
                <span class="b" style="background: #e6f9e6; border-color: #1b8a38;">
                    👋 ${sessionScope.currentUser.fullName}
                </span>
                <a href="${pageContext.request.contextPath}/auth/logout" class="b">Đăng xuất</a>
            </c:when>
            <c:otherwise>
                <button class="b" id="nl">Đăng nhập</button>
                <button class="b p" id="btnRegisterNav">Đăng ký</button>
            </c:otherwise>
        </c:choose>
    </div>
</nav>

<main class="w">
    <!-- HERO: SLIDER ẢNH SỰ KIỆN + MASCOT TICKIE -->
    <section class="hero reveal">
        <div class="hero-bg" aria-hidden="true">
            <i class="blob b1"></i><i class="blob b2"></i><i class="blob b3"></i>
            <i class="dot d1"></i><i class="dot d2"></i><i class="dot d3"></i><i class="dot d4"></i><i class="dot d5"></i>
            <i class="star s1">✦</i><i class="star s2">✦</i><i class="star s3">✦</i><i class="star s4">✦</i>
        </div>

        <div class="hero-text">
            <span class="tag" id="heroTag">${featuredEvent != null ? featuredEvent.tagLabel : 'NỔI BẬT · 20/07'}</span>
            <h1 id="heroTitle">${featuredEvent != null ? featuredEvent.title : 'ĐÊM NHẠC MÙA HÈ 2026'}</h1>
            <p id="heroDesc">${featuredEvent != null ? featuredEvent.description : 'Chọn sự kiện yêu thích, đặt vé trong 1 phút và nhận vé điện tử ngay.'}</p>
            <div class="cta">
                <button class="buy" id="buy" type="button" onclick="openOrderModal(window.heroEventId)">MUA VÉ</button>
                <span class="price">Từ <span id="heroPrice">${featuredEvent != null ? featuredEvent.formattedPrice : '350.000đ'}</span></span>
            </div>
            <div class="hero-meta">
                <span id="heroDate">🗓 ${featuredEvent != null ? featuredEvent.fullDateStr : ''}</span>
                <span id="heroLoc">📍 ${featuredEvent != null ? featuredEvent.location : ''}</span>
            </div>
        </div>

        <div class="hero-visual">
            <div class="poster-wrap" id="posterWrap">
                <span class="poster-back pb1"></span>
                <span class="poster-back pb2"></span>
                <c:choose>
                    <c:when test="${not empty eventList}">
                        <c:forEach items="${eventList}" var="e" varStatus="st">
                            <figure class="poster ${(featuredEvent != null && e.eventId == featuredEvent.eventId) ? 'on' : ''}"
                                    data-id="${e.eventId}" data-title="<c:out value='${e.title}'/>" data-desc="<c:out value='${e.description}'/>"
                                    data-tag="<c:out value='${e.tagLabel}'/>" data-price="${e.formattedPrice}"
                                    data-date="${e.fullDateStr}" data-loc="<c:out value='${e.location}'/>"
                                    onclick="openOrderModal(${e.eventId})">
                                <img src="${pageContext.request.contextPath}/${e.imagePath}" alt="${e.title}" ${st.index == 0 ? '' : 'loading="lazy"'}>
                                <figcaption><b>${e.title}</b><small>${e.categoryName}</small></figcaption>
                            </figure>
                        </c:forEach>
                    </c:when>
                    <c:when test="${featuredEvent != null}">
                        <figure class="poster on" data-id="${featuredEvent.eventId}" data-title="<c:out value='${featuredEvent.title}'/>"
                                data-desc="<c:out value='${featuredEvent.description}'/>" data-tag="<c:out value='${featuredEvent.tagLabel}'/>"
                                data-price="${featuredEvent.formattedPrice}" data-date="${featuredEvent.fullDateStr}"
                                data-loc="<c:out value='${featuredEvent.location}'/>" onclick="openOrderModal(${featuredEvent.eventId})">
                            <img src="${pageContext.request.contextPath}/${featuredEvent.imagePath}" alt="${featuredEvent.title}">
                            <figcaption><b>${featuredEvent.title}</b><small>${featuredEvent.categoryName}</small></figcaption>
                        </figure>
                    </c:when>
                </c:choose>
                <span class="poster-sticker">HOT 🔥</span>
            </div>
            <div class="poster-dots" id="posterDots" aria-label="Chọn sự kiện nổi bật"></div>

            <div id="heroSlot">
                <div class="stage">
                <div id="tilt">
                    <svg viewBox="0 0 400 480" xmlns="http://www.w3.org/2000/svg">
                        <defs>
                            <radialGradient id="body" cx=".32" cy=".25" r=".95">
                                <stop offset="0" stop-color="#e4ff6a"/>
                                <stop offset=".5" stop-color="#c8f51f"/>
                                <stop offset="1" stop-color="#a3d80e"/>
                            </radialGradient>
                            <radialGradient id="eye" cx=".38" cy=".3" r=".8">
                                <stop offset="0" stop-color="#fff"/>
                                <stop offset="1" stop-color="#dfe6f3"/>
                            </radialGradient>
                            <radialGradient id="yel" cx=".32" cy=".25" r=".9">
                                <stop offset="0" stop-color="#fff3a0"/>
                                <stop offset=".55" stop-color="#ffd400"/>
                                <stop offset="1" stop-color="#e0a000"/>
                            </radialGradient>
                            <radialGradient id="pnk" cx=".32" cy=".25" r=".9">
                                <stop offset="0" stop-color="#ffc4e2"/>
                                <stop offset=".55" stop-color="#ff62aa"/>
                                <stop offset="1" stop-color="#e03a8a"/>
                            </radialGradient>
                            <linearGradient id="shade" x1="0" y1="0" x2="0" y2="1">
                                <stop offset=".65" stop-color="#4a7a00" stop-opacity="0"/>
                                <stop offset="1" stop-color="#4a7a00" stop-opacity=".35"/>
                            </linearGradient>
                            <filter id="blur2"><feGaussianBlur stdDeviation="2.5"/></filter>
                            <filter id="drop"><feDropShadow dx="0" dy="6" stdDeviation="4" flood-opacity=".28"/></filter>
                        </defs>
                        <ellipse class="shadow" cx="200" cy="436" rx="135" ry="13" fill="#0a0a0a" opacity=".9"/>
                        <g filter="url(#drop)"><g class="bob">
                            <!-- legs -->
                            <rect x="154" y="320" width="12" height="70" rx="6" fill="#111"/>
                            <rect x="244" y="320" width="12" height="70" rx="6" fill="#111"/>
                            <!-- feet -->
                            <ellipse cx="156" cy="394" rx="38" ry="15" fill="url(#pnk)" stroke="#111" stroke-width="7"/>
                            <ellipse cx="146" cy="389" rx="12" ry="3.5" fill="#fff" opacity=".7" filter="url(#blur2)"/>
                            <ellipse cx="258" cy="394" rx="38" ry="15" fill="url(#yel)" stroke="#111" stroke-width="7"/>
                            <ellipse cx="248" cy="389" rx="12" ry="3.5" fill="#fff" opacity=".7" filter="url(#blur2)"/>

                            <!-- ticket body -->
                            <rect x="95" y="90" width="210" height="235" rx="32" fill="#111" transform="translate(13,13)"/>
                            <rect x="95" y="90" width="210" height="235" rx="32" fill="#4f8000" stroke="#111" stroke-width="8" transform="translate(6,6)"/>
                            <rect x="95" y="90" width="210" height="235" rx="32" fill="url(#body)" stroke="#111" stroke-width="8"/>
                            <rect x="95" y="90" width="210" height="235" rx="32" fill="url(#shade)"/>
                            <ellipse cx="135" cy="116" rx="30" ry="8" fill="#fff" opacity=".6" transform="rotate(-22 135 116)" filter="url(#blur2)"/>
                            <path d="M106 160 Q103 135 116 118" stroke="#fff" stroke-width="5" stroke-linecap="round" fill="none" opacity=".45"/>

                            <!-- notches -->
                            <circle cx="95" cy="232" r="15" fill="#111"/><circle cx="99" cy="232" r="9" fill="#26418f"/>
                            <circle cx="305" cy="232" r="15" fill="#111"/><circle cx="301" cy="232" r="9" fill="#26418f"/>
                            <line x1="140" y1="124" x2="250" y2="124" stroke="#111" stroke-width="6" stroke-dasharray="12 10" stroke-linecap="round"/>

                            <!-- cheeks -->
                            <ellipse cx="127" cy="230" rx="18" ry="11" fill="#ff62aa" opacity=".9"/>
                            <ellipse cx="127" cy="226" rx="8" ry="3" fill="#fff" opacity=".35" filter="url(#blur2)"/>
                            <ellipse cx="273" cy="230" rx="18" ry="11" fill="#ff62aa" opacity=".9"/>
                            <ellipse cx="273" cy="226" rx="8" ry="3" fill="#fff" opacity=".35" filter="url(#blur2)"/>

                            <!-- eyes -->
                            <g id="eyes">
                                <g><circle cx="148" cy="190" r="29" fill="#111"/><circle cx="148" cy="190" r="23" fill="url(#eye)"/>
                                    <g class="pupil"><circle cx="154" cy="194" r="12" fill="#111"/><circle cx="158" cy="189" r="4" fill="#fff"/></g>
                                    <circle cx="148" cy="190" r="25" fill="#b6ee1c" class="lid"/></g>
                                <g><circle cx="252" cy="190" r="29" fill="#111"/><circle cx="252" cy="190" r="23" fill="url(#eye)"/>
                                    <g class="pupil"><circle cx="258" cy="194" r="12" fill="#111"/><circle cx="262" cy="189" r="4" fill="#fff"/></g>
                                    <circle cx="252" cy="190" r="25" fill="#b6ee1c" class="lid"/></g>
                            </g>

                            <!-- half-disc smile -->
                            <path d="M160 224 H240 Q236 258 200 258 Q164 258 160 224Z" fill="#111" stroke="#111" stroke-width="4" stroke-linejoin="round"/>
                            <path d="M178 249 Q200 238 222 249 Q212 257 200 257 Q188 257 178 249Z" fill="url(#pnk)"/>

                            <!-- T mark -->
                            <g fill="#3f6a00" transform="translate(3,3)"><rect x="176" y="276" width="48" height="14"/><rect x="193" y="290" width="14" height="24"/></g>
                            <g fill="#111"><rect x="176" y="276" width="48" height="14"/><rect x="193" y="290" width="14" height="24"/></g>

                            <!-- arms -->
                            <g class="wave">
                                <path d="M93 236 C48 232 34 192 50 146" stroke="#111" stroke-width="12" stroke-linecap="round" fill="none"/>
                                <circle cx="50" cy="130" r="19" fill="url(#yel)" stroke="#111" stroke-width="8"/><ellipse cx="44" cy="124" rx="5" ry="3" fill="#fff" opacity=".85"/>
                            </g>
                            <g class="arm2">
                                <path d="M307 236 C350 230 364 262 358 300" stroke="#111" stroke-width="12" stroke-linecap="round" fill="none"/>
                                <circle cx="358" cy="316" r="19" fill="url(#pnk)" stroke="#111" stroke-width="8"/><ellipse cx="352" cy="310" rx="5" ry="3" fill="#fff" opacity=".85"/>
                            </g>

                            <!-- bubble -->
                            <g class="bubble">
                                <rect x="222" y="30" width="128" height="42" rx="12" fill="#111" transform="translate(6,6)"/>
                                <rect x="222" y="30" width="128" height="42" rx="12" fill="url(#yel)" stroke="#111" stroke-width="5"/>
                                <rect x="230" y="35" width="112" height="8" rx="4" fill="#fff" opacity=".4"/>
                                <text id="say" x="286" y="59" text-anchor="middle" font-size="20" font-weight="800" fill="#111" font-family="Arial,sans-serif">Mua vé nào!</text>
                            </g>
                        </g></g>
                    </svg>
                </div>
            </div>
            </div>
        </div>
    </section>

    <!-- DẢI CHỮ CHẠY MÀU SẮC -->
    <div class="ticker reveal" aria-hidden="true">
        <div class="ticker-track">
            <span>🎵 ÂM NHẠC</span><span>⚽ THỂ THAO</span><span>🎤 HỘI THẢO</span><span>🎭 SÂN KHẤU</span><span>🎟️ MUA VÉ NGAY</span><span>✨ ƯU ĐÃI ${siteInfo.coupon_code}</span>
            <span>🎵 ÂM NHẠC</span><span>⚽ THỂ THAO</span><span>🎤 HỘI THẢO</span><span>🎭 SÂN KHẤU</span><span>🎟️ MUA VÉ NGAY</span><span>✨ ƯU ĐÃI ${siteInfo.coupon_code}</span>
        </div>
    </div>

    <!-- SECTION DANH MỤC VÀ DANH SÁCH SỰ KIỆN TỪ SQL SERVER -->
    <div class="sec reveal" id="ev">
        <h2>SẮP DIỄN RA</h2>
        <!-- CÁC CHIP DANH MỤC LẤY ĐỘNG TỪ BẢNG Categories -->
        <div class="chips" id="chips">
            <a href="${pageContext.request.contextPath}/home#ev" 
               class="chip ${empty param.category || param.category == 'Tất cả' ? 'on' : ''}">
                Tất cả
            </a>
            <c:forEach items="${categories}" var="c">
                <a href="${pageContext.request.contextPath}/home?category=${c.categoryName}#ev" 
                   class="chip ${param.category == c.categoryName ? 'on' : ''}">
                    ${c.categoryName}
                </a>
            </c:forEach>
        </div>
    </div>

    <!-- GRID SỰ KIỆN LẤY ĐỘNG TỪ BẢNG Events TRONG SQL SERVER (HOÀN TOÀN KHÔNG DÙNG DỮ LIỆU CỨNG) -->
    <div class="grid" id="grid">
        <c:forEach items="${eventList}" var="e" varStatus="st">
            <article class="ev reveal" style="--i:${st.index}; --cat:${e.categoryColor};"
                     onclick="openOrderModal(${e.eventId})" data-category="${e.categoryName}" data-title="${e.title}">
                <div class="ev-img">
                    <img src="${pageContext.request.contextPath}/${e.imagePath}" alt="${e.title}" loading="lazy">
                    <span class="ev-cat">${e.categoryName}</span>
                </div>
                <div class="ev-body">
                    <h3>${e.title}</h3>
                    <div class="m">📍 ${e.location}</div>
                    <div class="m">🗓 ${e.fullDateStr}</div>
                    <div class="ev-foot">
                        <span class="pr">Từ ${e.formattedPrice}</span>
                        <span class="ev-go">Mua vé →</span>
                    </div>
                </div>
            </article>
        </c:forEach>
        <c:if test="${empty eventList}">
            <div class="empty-state">
                <b>😅 Chưa tìm thấy sự kiện phù hợp</b>
                <span>Thử từ khóa khác hoặc chọn danh mục "Tất cả" nhé!</span>
            </div>
        </c:if>
    </div>

    <!-- BANNER DÀNH CHO NHÀ TỔ CHỨC SỰ KIỆN -->
    <div class="organizer-banner reveal">
        <div>
            <h3>Bạn tổ chức sự kiện?</h3>
            <p>Mở bán vé, theo dõi doanh thu và soát vé QR tại một nơi trên TicketHub Dashboard.</p>
        </div>
        <c:choose>
            <c:when test="${sessionScope.currentUser.role == 'ADMIN'}">
                <a href="${pageContext.request.contextPath}/dashboard" class="buy" style="text-decoration: none; display: inline-flex; align-items: center; justify-content: center;">
                    Đăng sự kiện
                </a>
            </c:when>
            <c:otherwise>
                <a href="mailto:${siteInfo.support_email}?subject=Đăng%20sự%20kiện%20trên%20TicketHub" class="buy" style="text-decoration: none; display: inline-flex; align-items: center; justify-content: center;">
                    Liên hệ đăng sự kiện
                </a>
            </c:otherwise>
        </c:choose>
    </div>
</main>

<!-- FOOTER -->
<footer class="site-footer">
    <div class="w">
        <div class="ft-grid">
            <div class="ft-col ft-brand">
                <a href="${pageContext.request.contextPath}/home" class="logo-container" title="TicketHub">
                    <div class="logo-badge">
                        <img src="${pageContext.request.contextPath}/assets/images/logo.svg" alt="TicketHub Logo">
                    </div>
                    <div class="logo-pill">TICKETHUB</div>
                </a>
                <p>Nền tảng bán vé sự kiện trực tuyến: âm nhạc, thể thao, hội thảo, sân khấu. Chọn vé, thanh toán và nhận vé điện tử ngay trong vài phút.</p>
                <div class="ft-chips">
                    <span>🎵 Âm nhạc</span><span>⚽ Thể thao</span><span>🎤 Hội thảo</span><span>🎭 Sân khấu</span>
                </div>
            </div>

            <div class="ft-col">
                <h4>Khám phá</h4>
                <ul>
                    <li><a href="${pageContext.request.contextPath}/home">Trang chủ</a></li>
                    <li><a href="${pageContext.request.contextPath}/home#ev">Sự kiện sắp diễn ra</a></li>
                    <c:if test="${sessionScope.currentUser.role != 'ADMIN'}">
                        <li><a href="${pageContext.request.contextPath}/orders">Vé của tôi</a></li>
                    </c:if>
                    <c:if test="${sessionScope.currentUser.role == 'ADMIN'}">
                        <li><a href="${pageContext.request.contextPath}/dashboard">Dashboard quản trị</a></li>
                    </c:if>
                    <c:forEach items="${categories}" var="c">
                        <li><a href="${pageContext.request.contextPath}/home?category=${c.categoryName}#ev">${c.categoryName}</a></li>
                    </c:forEach>
                </ul>
            </div>

            <div class="ft-col">
                <h4>Mua vé trong 3 bước</h4>
                <ol class="ft-steps">
                    <li><b>Chọn sự kiện</b><span>và hạng vé, số lượng (tối đa 10 vé/loại).</span></li>
                    <li><b>Điền thông tin</b><span>người nhận, chọn cách thanh toán.</span></li>
                    <li><b>Nhận vé</b><span>mã vé &amp; đơn hàng được lưu ngay vào hệ thống.</span></li>
                </ol>
                <h4 class="ft-h4-gap">Thanh toán</h4>
                <div class="ft-chips">
                    <span>Thẻ ATM</span><span>QR</span><span>Ví điện tử</span><span>Visa / Mastercard</span>
                </div>
            </div>

            <div class="ft-col">
                <h4>Liên hệ &amp; hỗ trợ</h4>
                <ul class="ft-contact">
                    <li>📍 ${siteInfo.address}</li>
                    <li>📞 Hotline: <b>${siteInfo.hotline}</b></li>
                    <li>✉️ <a href="mailto:${siteInfo.support_email}">${siteInfo.support_email}</a></li>
                    <li>🕘 Hỗ trợ ${siteInfo.support_hours}</li>
                </ul>
                <h4 class="ft-h4-gap">Quyền lợi &amp; chính sách</h4>
                <ul class="ft-contact">
                    <li>🎁 Mã <b>${siteInfo.coupon_code}</b>: giảm ${siteInfo.coupon_percent}% giá vé</li>
                    <li>💳 Phí dịch vụ: <span id="ftFee"></span> / vé</li>
                    <li>🔒 Thông tin được bảo mật, thanh toán an toàn</li>
                </ul>
            </div>
        </div>

        <div class="ft-bottom">
            <span>© 2026 TicketHub. Hệ thống đặt vé sự kiện trực tuyến.</span>
            <span class="ft-tech">Java Servlet / JSP (MVC) · SQL Server · Apache Tomcat 10.1 · JDK 17</span>
        </div>
    </div>
</footer>

<!-- MODAL ĐĂNG NHẬP -->
<div id="login">
    <button class="x" id="cl" aria-label="Đóng">✕</button>
    <div class="lp">
        <span class="tag">XIN CHÀO BẠN MỚI!</span>
        <h2>CHÀO MỪNG<br>TRỞ LẠI</h2>
        <div id="slot"></div>
    </div>
    <div class="rp">
        <div class="fc" id="fc">
            <h3>ĐĂNG NHẬP</h3>
            <label for="em" style="margin-top:8px">Email hoặc số điện thoại</label>
            <div class="f">
                <input id="em" type="text" placeholder="ban@email.com" autocomplete="username">
            </div>
            <label for="pw">Mật khẩu</label>
            <div class="f">
                <input id="pw" type="password" placeholder="••••••••" autocomplete="current-password" style="padding-right:76px">
                <button type="button" class="eyb" id="tg">HIỆN</button>
            </div>
            <div class="rw">
                <label class="chk"><input type="checkbox" checked> Ghi nhớ tôi</label>
                <a href="javascript:void(0)" onclick="toast('Gợi ý: admin@tickethub.vn / admin123')">Quên mật khẩu?</a>
            </div>
            <button class="go" id="go">ĐĂNG NHẬP</button>
            <div class="or">HOẶC</div>
            <div class="soc">
                <button type="button" onclick="toast('Đăng nhập Google')">Google</button>
                <button type="button" onclick="toast('Đăng nhập Apple')">Apple</button>
            </div>
            <div class="rg">Chưa có tài khoản? <a href="javascript:void(0)" onclick="openR()">Đăng ký</a></div>
        </div>
    </div>
</div>

<!-- MODAL ĐĂNG KÝ TÀI KHOẢN -->
<div id="reg">
    <button class="x" id="cr" aria-label="Đóng" onclick="closeL()">✕</button>
    <div class="lp">
        <span class="tag">THÀNH VIÊN MỚI!</span>
        <h2>THAM GIA<br>CÙNG TICKIE</h2>
        <div id="slot2"></div>
    </div>
    <div class="rp">
        <div class="fc" id="fc2">
            <h3>TẠO TÀI KHOẢN</h3>
            <label for="rn" style="margin-top:8px">Họ và tên</label>
            <div class="f"><input id="rn" type="text" placeholder="Nguyễn Văn A" autocomplete="name"></div>
            <label for="re">Email</label>
            <div class="f"><input id="re" type="text" inputmode="email" placeholder="ban@email.com" autocomplete="email"></div>
            <label for="rp">Số điện thoại</label>
            <div class="f"><input id="rp" type="text" inputmode="tel" placeholder="09xx xxx xxx" autocomplete="tel"></div>
            <label for="rw">Mật khẩu</label>
            <div class="f"><input id="rw" type="password" placeholder="Tối thiểu 6 ký tự" autocomplete="new-password" style="padding-right:76px">
                <button type="button" class="eyb" id="tg2">HIỆN</button>
            </div>
            <div class="sm" id="sm"><i></i><i></i><i></i><i></i></div>
            <div class="sl" id="sl"></div>
            <label for="rc">Nhập lại mật khẩu</label>
            <div class="f"><input id="rc" type="password" placeholder="••••••••" autocomplete="new-password"></div>
            <label class="chk">
                <input type="checkbox" id="ag" checked>
                <span>Tôi đồng ý với <a class="t" href="#">Điều khoản</a> và <a class="t" href="#">Chính sách</a> của TicketHub</span>
            </label>
            <button class="go" id="go2" style="margin-top:18px">TẠO TÀI KHOẢN</button>
            <div class="or">HOẶC</div>
            <div class="soc">
                <button type="button" onclick="toast('Đăng ký Google')">Google</button>
                <button type="button" onclick="toast('Đăng ký Apple')">Apple</button>
            </div>
            <div class="rg">Đã có tài khoản? <a href="javascript:void(0)" id="toL" onclick="openL()">Đăng nhập</a></div>
        </div>
    </div>
</div>

<!-- MODAL ĐẶT VÉ VÀ THANH TOÁN -->
<div id="pay">
    <button class="x" id="cx" aria-label="Đóng">✕</button>
    <div class="ph">
        <div class="w">
            <div class="logo-container">
                <div class="logo-badge" style="width: 38px; height: 38px;">
                    <img src="${pageContext.request.contextPath}/assets/images/logo.svg" alt="TicketHub Logo">
                </div>
                <div class="logo-pill" style="font-size: 16px; padding: 4px 10px;">TICKETHUB</div>
            </div>
            <div class="stp">
                <span class="on">1 Chọn vé</span>
                <span class="on">2 Thanh toán</span>
                <span id="s3">3 Hoàn tất</span>
            </div>
        </div>
    </div>

    <div class="w">
        <div class="pg" id="pg">
            <div>
                <!-- Bước 1: Chọn vé -->
                <div class="fc pc">
                    <h3><span class="nb">1</span>Chọn vé</h3>
                    <div id="evn" style="font-weight:700;color:var(--mut)"></div>
                    <div id="tks"></div>
                </div>

                <!-- Bước 2: Thông tin người nhận -->
                <div class="fc pc">
                    <h3><span class="nb">2</span>Thông tin người nhận</h3>
                    <label for="pn">Họ và tên</label>
                    <div class="f">
                        <input id="pn" type="text" placeholder="Nguyễn Văn A" 
                               value="${sessionScope.currentUser != null ? sessionScope.currentUser.fullName : ''}">
                    </div>
                    <div class="g2">
                        <div>
                            <label for="pe">Email nhận vé</label>
                            <div class="f">
                                <input id="pe" type="text" placeholder="ban@email.com" 
                                       value="${sessionScope.currentUser != null ? sessionScope.currentUser.email : ''}">
                            </div>
                        </div>
                        <div>
                            <label for="pt">Số điện thoại</label>
                            <div class="f">
                                <input id="pt" type="text" placeholder="09xx xxx xxx" 
                                       value="${sessionScope.currentUser != null ? sessionScope.currentUser.phone : ''}">
                            </div>
                        </div>
                    </div>
                </div>

                <!-- Bước 3: Phương thức thanh toán -->
                <div class="fc pc">
                    <h3><span class="nb">3</span>Phương thức thanh toán</h3>
                    <div class="pm" id="pm">
                        <button class="on" data-m="bank">ATM / Tài khoản ngân hàng<small>Qua cổng VNPay · nội địa</small></button>
                        <button data-m="qr">VNPay QR<small>Quét bằng app ngân hàng</small></button>
                        <button data-m="intl">Thẻ quốc tế<small>Visa / Mastercard / JCB</small></button>
                    </div>
                    <div id="vnp" style="margin-top:14px;font-weight:600;line-height:1.6">
                        Bạn sẽ được chuyển sang cổng thanh toán <b>VNPay (Sandbox – môi trường thử nghiệm)</b>
                        để hoàn tất giao dịch. Không có tiền thật bị trừ.
                        <div style="margin-top:10px;padding:12px 14px;border:2.5px dashed var(--ink);border-radius:14px;background:#fffbe6;font-size:13px">
                            <b>Thẻ test (ngân hàng NCB):</b><br>
                            Số thẻ: <b>9704198526191432198</b><br>
                            Tên chủ thẻ: <b>NGUYEN VAN A</b> · Ngày phát hành: <b>07/15</b><br>
                            Mật khẩu OTP: <b>123456</b>
                        </div>
                    </div>
                </div>
            </div>

            <!-- Tóm tắt đơn hàng bên phải -->
            <div class="sum">
                <div id="slot3"></div>
                <div class="fc">
                    <h3>Đơn hàng</h3>
                    <div id="lines"></div>
                    <label for="cpn">Mã giảm giá <small style="font-weight:600;color:var(--mut)">(thử ${siteInfo.coupon_code})</small></label>
                    <div class="cp">
                        <input id="cpn" type="text" placeholder="Nhập mã">
                        <button type="button" id="ap">ÁP DỤNG</button>
                    </div>
                    <div class="tot">
                        <span>Tổng cộng</span>
                        <span id="tot">0đ</span>
                    </div>
                    <button class="go" id="pb" style="margin-top:18px">THANH TOÁN QUA VNPAY</button>
                </div>
            </div>
        </div>

        <!-- MÀN HÌNH HOÀN TẤT ĐẶT VÉ -->
        <div id="ok">
            <div id="slot4"></div>
            <h1>THANH TOÁN THÀNH CÔNG!</h1>
            <div style="font-weight:600">Vé và mã đơn hàng đã được lưu vào hệ thống cơ sở dữ liệu.</div>
            <div class="tkt">
                <div class="a">
                    <h3 id="ot"></h3>
                    <p id="od"></p>
                    <p id="oq"></p>
                    <p id="oh"></p>
                </div>
                <div class="b2">
                    <div>
                        <div style="font-size:12px">MÃ VÉ</div>
                        <div id="oc" style="font-size:20px"></div>
                    </div>
                </div>
            </div>
            <button class="go" id="hm" style="max-width:320px" onclick="closeL()">VỀ TRANG CHỦ</button>
        </div>
    </div>
</div>

<div id="toast"></div>

<!-- JAVASCRIPT XỬ LÝ SỰ KIỆN VÀ AJAX VỚI SERVLET -->
<script>
    window.APP_CONTEXT = '${pageContext.request.contextPath}';
    window.IS_LOGGED_IN = ${not empty sessionScope.currentUser};
    window.USER_ROLE = '${not empty sessionScope.currentUser ? sessionScope.currentUser.role : ""}';
    window.SERVICE_FEE = ${siteInfo.service_fee};
    window.COUPON_CODE = '${siteInfo.coupon_code}';
    window.COUPON_PERCENT = ${siteInfo.coupon_percent};
    document.addEventListener('DOMContentLoaded', function () { var f = document.getElementById('ftFee'); if (f) f.textContent = new Intl.NumberFormat('vi-VN').format(window.SERVICE_FEE) + 'đ'; });
    window.heroEventId = ${featuredEvent != null ? featuredEvent.eventId : 1};
</script>
<script src="${pageContext.request.contextPath}/assets/js/page.js"></script>
<script src="${pageContext.request.contextPath}/assets/js/app.js"></script>
</body>
</html>
