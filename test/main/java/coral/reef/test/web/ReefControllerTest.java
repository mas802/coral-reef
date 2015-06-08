/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package coral.reef.test.web;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.web.servlet.HandlerMapping;

import static org.mockito.Mockito.*;
import coral.reef.service.ReefHandler;
import coral.reef.service.ReefService;
import coral.reef.web.ReefToCoralController;

@RunWith(MockitoJUnitRunner.class)
public class ReefControllerTest {

    @InjectMocks
    ReefToCoralController reefController = new ReefToCoralController();

    @Mock
    ReefService reefService;

    @Test
    public void testUrl() throws IOException {

        final HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        when(
                httpRequest
                        .getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE))
                .thenReturn("/lineup/world.xml");
        final HttpServletResponse httpResponse = mock(HttpServletResponse.class);
        final HttpSession httpSession = mock(HttpSession.class);

        final ReefHandler handler = mock(ReefHandler.class);

        when(reefService.handler("test")).thenReturn(handler);
        when(handler.startMarker()).thenReturn("test");
        when(handler.refreshMarker()).thenReturn("test");
        when(handler.processMarker()).thenReturn("test");
        when(handler.serverMarker()).thenReturn("test");

        reefController.dispatchToExpHandler("test", httpSession, httpResponse,
                httpRequest);

    }
}
