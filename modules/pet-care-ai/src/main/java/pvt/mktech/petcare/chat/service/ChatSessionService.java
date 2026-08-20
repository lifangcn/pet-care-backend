package pvt.mktech.petcare.chat.service;

import cn.hutool.core.util.IdUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pvt.mktech.petcare.chat.dto.request.CreateSessionRequest;
import pvt.mktech.petcare.chat.dto.response.*;
import pvt.mktech.petcare.chat.repository.ChatHistoryRepository;
import pvt.mktech.petcare.entity.ChatMessageDocument;
import java.time.Instant;
import java.util.List;

@Service @RequiredArgsConstructor
public class ChatSessionService {
    private static final int MAX_PAGE_SIZE=100;
    private final ChatHistoryRepository history;
    public SessionResponse createSession(Long userId,CreateSessionRequest request) { if(request==null) throw new IllegalArgumentException("创建会话请求不能为空"); String id=IdUtil.simpleUUID(); String name=request.getName()!=null&&!request.getName().isBlank()?request.getName():ChatHistoryRepository.DEFAULT_SESSION_NAME; history.createSession(userId,id,name); var session=history.getSession(userId,id).orElseThrow(()->new IllegalStateException("创建会话失败")); return new SessionResponse(session.getId(),session.getName(),session.getCreatedAt(),session.getUpdatedAt()); }
    public SessionListResponse listSessions(Long userId,Long pageNumber,Long pageSize) { long page=Math.max(1L,pageNumber==null?1L:pageNumber); long requestedSize=Math.max(1L,pageSize==null?10L:pageSize); int size=(int)Math.min(MAX_PAGE_SIZE,requestedSize); if(page-1>Integer.MAX_VALUE/(long)size) throw new IllegalArgumentException("页码超出范围"); return new SessionListResponse(history.countSessions(userId),history.listSessions(userId,(int)((page-1)*size),size)); }
    public List<ChatMessageResponse> getSessionMessages(Long userId,String id,int limit) { requireSession(userId,id); return history.getSessionHistory(userId,id,Math.min(1000,Math.max(1,limit))).stream().map(this::response).toList(); }
    public void deleteSession(Long userId,String id) { requireSession(userId,id); history.deleteBySessionId(userId,id); }
    private void requireSession(Long userId,String id) { if(history.getSession(userId,id).isEmpty()) throw new IllegalArgumentException("会话不存在或无权访问"); }
    private ChatMessageResponse response(ChatMessageDocument d) { return new ChatMessageResponse(d.getId(),d.getRole()==null?"user":d.getRole().toLowerCase(),d.getContent(),d.getCreatedAt()); }
}
