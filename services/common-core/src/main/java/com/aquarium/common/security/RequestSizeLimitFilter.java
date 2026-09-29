package com.aquarium.common.security;

import com.aquarium.common.exception.ErrorCode;
import com.aquarium.common.exception.PayloadTooLargeException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * Chặn request có body quá lớn (chống DoS / phình DB).
 * - Có Content-Length vượt ngưỡng: trả 413 ngay.
 * - Không có Content-Length (chunked): đếm byte khi đọc, vượt ngưỡng thì ném lỗi 413.
 */
public class RequestSizeLimitFilter extends OncePerRequestFilter {

    private static final Set<String> BODY_METHODS = Set.of("POST", "PUT", "PATCH");

    private final long maxBytes;

    public RequestSizeLimitFilter(long maxBytes) {
        this.maxBytes = maxBytes;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long contentLength = request.getContentLengthLong();
        if (contentLength > maxBytes) {
            response.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\":false,\"code\":" + ErrorCode.PAYLOAD_TOO_LARGE.getCode()
                    + ",\"message\":\"" + ErrorCode.PAYLOAD_TOO_LARGE.getMessage() + "\"}");
            return;
        }
        if (contentLength < 0 && BODY_METHODS.contains(request.getMethod())) {
            filterChain.doFilter(new LimitedBodyRequest(request, maxBytes), response);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private static final class LimitedBodyRequest extends HttpServletRequestWrapper {
        private final long limit;
        private ServletInputStream stream;

        LimitedBodyRequest(HttpServletRequest request, long limit) {
            super(request);
            this.limit = limit;
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            if (stream == null) {
                stream = new LimitedServletInputStream(super.getInputStream(), limit);
            }
            return stream;
        }

        @Override
        public BufferedReader getReader() throws IOException {
            String encoding = getCharacterEncoding();
            Charset charset = encoding != null ? Charset.forName(encoding) : StandardCharsets.UTF_8;
            return new BufferedReader(new InputStreamReader(getInputStream(), charset));
        }
    }

    private static final class LimitedServletInputStream extends ServletInputStream {
        private final ServletInputStream delegate;
        private final long limit;
        private long count;

        LimitedServletInputStream(ServletInputStream delegate, long limit) {
            this.delegate = delegate;
            this.limit = limit;
        }

        @Override
        public int read() throws IOException {
            int b = delegate.read();
            if (b != -1) {
                track(1);
            }
            return b;
        }

        @Override
        public int read(byte[] buffer, int off, int len) throws IOException {
            int n = delegate.read(buffer, off, len);
            if (n > 0) {
                track(n);
            }
            return n;
        }

        private void track(int n) throws PayloadTooLargeException {
            count += n;
            if (count > limit) {
                throw new PayloadTooLargeException(limit);
            }
        }

        @Override
        public boolean isFinished() {
            return delegate.isFinished();
        }

        @Override
        public boolean isReady() {
            return delegate.isReady();
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            delegate.setReadListener(readListener);
        }
    }
}
