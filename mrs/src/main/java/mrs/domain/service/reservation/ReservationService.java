package mrs.domain.service.reservation;

import java.util.List;
import java.util.Objects;

import org.hibernate.UnsupportedLockAttemptException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mrs.domain.model.*;
import mrs.domain.repository.reservation.ReservationRepository;
import mrs.domain.repository.room.ReservableRoomRepository;

@Service
@Transactional
public class ReservationService {
	@Autowired
	ReservationRepository reservationRepository;
	@Autowired
	ReservableRoomRepository reservableRoomRepository;

	public Reservation reserve(Reservation reservation) {
		ReservableRoomId reservableRoomId = reservation.getReservableRoom().getReservableRoomId();
		// 対象の部屋が予約可能かチェック
		//悲観ロック
		ReservableRoom reservable = reservableRoomRepository.findById(reservableRoomId).orElseThrow();
		if (reservable == null) {
			throw new UnsupportedLockAttemptException("入力の日付・部屋の組み合わせは予約できません。");
		}
		// 重複チェック
		boolean	overlap = reservationRepository.findByReservableRoom_ReservableRoomIdOrderByStartTimeAsc(reservableRoomId).stream().anyMatch(x -> x.overlap(reservation));
		if(overlap) {
			throw new AlreadyReservedException("入力の時間帯はすでに予約済みです。");
		}

		// 予約情報の登録
		reservationRepository.save(reservation);
		return reservation;
	}

	public void cancel(Integer reservationId, User requestUser) {
		Reservation reservation = reservationRepository.findById(reservationId).orElseThrow();
		if(RoleName.ADMIN != requestUser.getRoleName() && !Objects.equals(reservation.getUser().getUserId(), requestUser.getUserId())) {
			throw new AccessDeniedException("要求されたキャンセルは許可できません。");
		}
		reservationRepository.delete(reservation);
	}


	public List<Reservation> findReservations(ReservableRoomId reservableRoomId) {
		return reservationRepository.findByReservableRoom_ReservableRoomIdOrderByStartTimeAsc(reservableRoomId);
	}
}
