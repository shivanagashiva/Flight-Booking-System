package com.flight.management.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.flight.management.model.Flight;
import com.flight.management.model.Reservation;
import com.flight.management.model.Seat;
import com.flight.management.model.User;
import com.flight.management.repository.FlightRepository;
import com.flight.management.repository.ReservationRepository;
import com.flight.management.repository.SeatRepository;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/user")
public class ReservationController {

    @Autowired
    private FlightRepository flightRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private SeatRepository seatRepository;

    
    @GetMapping("/dashboard_customer")
    public String dashboardCustomer() {
        return "dashboard_customer";
    }

    @GetMapping("/reservation/reservations")
    public String viewReservations(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        Date today = new Date();
        List<Reservation> allReservations = reservationRepository.findByUserId(user.getUserId());
        List<Reservation> upcomingReservations = allReservations.stream().filter(reservation -> !reservation.getDate().before(today)).collect(Collectors.toList());
        model.addAttribute("reservations", upcomingReservations);
        return "reservations";
    }

    @GetMapping("/reservation/my_bookings")
    public String viewPastFlights(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        List<Reservation> allReservations = reservationRepository.findByUserId(user.getUserId());
        model.addAttribute("allFlights", allReservations);
        return "my_bookings";
    }

    @GetMapping ("/reservation/add_reservation")
    public String createReservation(Model model) {
        model.addAttribute("reservationExist", false);
        model.addAttribute("flightExist", true);
        model.addAttribute("createSuccess", false);
        model.addAttribute("flight",new Flight());
        model.addAttribute("reservation",new Reservation());
        return "add_reservation";
    }

    @PostMapping ("/reservation/save_created_reservation")
    public String saveCreatedReservation(@ModelAttribute("flight") Flight flight,@ModelAttribute("reservation") Reservation reservation, Model model, HttpSession session) {
        User user = (User) session.getAttribute("user");
        model.addAttribute("reservationExist", false);
        model.addAttribute("flightExist", true);
        model.addAttribute("createSuccess", false);
        model.addAttribute("seatFull", false);
        model.addAttribute("seatOccupied", false);
        Flight savedFlight = flightRepository.findByFromAndToAndDate(flight.getFrom(), flight.getTo(), flight.getDate());
        if (savedFlight != null ) {
        	if(seatRepository.findAllByFlightId(savedFlight.getFlightId()).size()>=60) { //Assuming default seat availability is 60.
       		 model.addAttribute("seatFull", true);
        	}
        	else {
            if (!reservationRepository.existsByUserIdAndFlightIdAndSeatNumber(user.getUserId(), savedFlight.getFlightId(),reservation.getSeatNumber()))
            {
            	Seat seat=seatRepository.findByFlightIdAndSeatNumber(savedFlight.getFlightId(), reservation.getSeatNumber());
            	if (seat!= null && seat.isOccupied()) {
                    model.addAttribute("seatOccupied", true);
                }else {
                reservationRepository.save(Reservation.builder()
                        .userId(user.getUserId())
                        .userName(user.getUsername())
                        .flightId(savedFlight.getFlightId())
                        .from(savedFlight.getFrom())
                        .to(savedFlight.getTo())
                        .date(savedFlight.getDate())
                        .seatNumber(reservation.getSeatNumber()).build());
                seatRepository.save(Seat.builder().flightId(savedFlight.getFlightId()).seatNumber(reservation.getSeatNumber()).isOccupied(true).build());
                model.addAttribute("createSuccess", true);
                }} else {
                model.addAttribute("reservationExist", true);
            }}
        } else {
            model.addAttribute("flightExist", false);
        }
        return "add_reservation";
    }

    @PostMapping ("/reservation/modify_reservation")
    public String modifyReservation(@RequestParam("reservationId") Long reservationId, Model model) {
        Reservation reservation = reservationRepository.findById(reservationId).get();
        Seat seat=seatRepository.findByFlightIdAndSeatNumber(reservation.getFlightId(), reservation.getSeatNumber());
        model.addAttribute("seat", seat);
        model.addAttribute("selectedReservation", reservation);
        model.addAttribute("modifyReservationSuccess", false);
        model.addAttribute("flightExist", true);
        model.addAttribute("seatOccupied", false);
        return "modify_reservation";
    }

    @PostMapping ("/reservation/save_modified_reservation")
    public String saveModifiedReservation(@ModelAttribute("selectedReservation") Reservation reservation, @ModelAttribute("seat") Seat seat1, Model model, HttpSession session) {
    	User user = (User) session.getAttribute("user");
        model.addAttribute("modifyReservationSuccess", false);
        model.addAttribute("flightExist", true);
        model.addAttribute("seatOccupied", false);
        Flight flight = flightRepository.findByFromAndToAndDate(reservation.getFrom(), reservation.getTo(), reservation.getDate());
        Reservation savedReservation = reservationRepository.findById(reservation.getReservationId()).get();
        Seat seat=seatRepository.findByFlightIdAndSeatNumber(seat1.getFlightId(), seat1.getSeatNumber());
        if (flight != null) {
            if (reservation.getSeatNumber() != null) {
                if (seat!= null && seat.isOccupied()) {
                    model.addAttribute("seatOccupied", true);
                } else {
                    reservationRepository.save(Reservation.builder()
                            .reservationId(savedReservation.getReservationId())
                            .userId(user.getUserId())
                            .userName(user.getUsername())
                            .flightId(flight.getFlightId())
                            .from(flight.getFrom())
                            .to(flight.getTo())
                            .date(flight.getDate())
                            .seatNumber(reservation.getSeatNumber()).build());
                    seat=seatRepository.findById(seat1.getSeatId()).get();
                    	seat.setFlightId(savedReservation.getFlightId());
                        seat.setSeatNumber(reservation.getSeatNumber());
                        seat.setOccupied(true);
                        seatRepository.save(seat);
                    model.addAttribute("modifyReservationSuccess", true);
                }
            } else {
                reservationRepository.save(Reservation.builder()
                        .reservationId(savedReservation.getReservationId())
                        .userId(user.getUserId())
                        .userName(user.getUsername())
                        .flightId(flight.getFlightId())
                        .from(flight.getFrom())
                        .to(flight.getTo())
                        .date(flight.getDate())
                        .seatNumber(reservation.getSeatNumber()).build());
                model.addAttribute("modifyReservationSuccess", true);
            }
        } else {
            model.addAttribute("flightExist", false);
        }
        model.addAttribute("selectedReservation", reservation);
        return "modify_reservation";
    }

    @PostMapping ("/reservation/delete_reservation")
    public String deleteReservation(@RequestParam("reservationId") Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId).get();
        reservationRepository.deleteById(reservation.getReservationId());
        Seat seat=seatRepository.findByFlightIdAndSeatNumber(reservation.getFlightId(), reservation.getSeatNumber());
        seatRepository.delete(seat);
        return "redirect:user/reservation/reservations";
    }
    
    @GetMapping("/searchflights")
    public String searchflights(Model model) {
    	Date today = new Date();
    	List<Flight> flights = flightRepository.findAll();
        List<Flight> flight = flights.stream().filter(fly -> !fly.getDate().before(today)).collect(Collectors.toList());
    	model.addAttribute("flight", flight);
    	model.addAttribute("flightobj", new Flight());
    	return "search_flights";
     }
    
    @PostMapping("/searchflights")
    public String searchflightsByDate(@ModelAttribute("flightobj") Flight flight,Model model) {
    	List<Flight> flights=flightRepository.findByDate(flight.getDate());
    	System.out.println(flights.toString());
    	model.addAttribute("flight", flights);
    	return "search_flights";
     }
}
