package com.flightbooking.controller;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.flightbooking.model.Flight;
import com.flightbooking.model.Reservation;
import com.flightbooking.model.Seat;
import com.flightbooking.model.User;
import com.flightbooking.repository.FlightRepository;
import com.flightbooking.repository.ReservationRepository;
import com.flightbooking.repository.SeatRepository;

import jakarta.servlet.http.HttpSession;

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
        List<Reservation> upcomingReservations = allReservations.stream().filter(res -> !res.getDate().before(today)).collect(Collectors.toList());
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

    @GetMapping("/reservation/add_reservation")
    public String createReservation(Model model) {
        model.addAttribute("reservationExist", false);
        model.addAttribute("flightExist", true);
        model.addAttribute("createSuccess", false);
        model.addAttribute("flight", new Flight());
        model.addAttribute("reservation", new Reservation());
        return "add_reservation";
    }

    @PostMapping("/reservation/save_created_reservation")
    public String saveCreatedReservation(@ModelAttribute("flight") Flight flight, @ModelAttribute("reservation") Reservation reservation, Model model, HttpSession session) {
        User user = (User) session.getAttribute("user");
        model.addAttribute("reservationExist", false);
        model.addAttribute("flightExist", true);
        model.addAttribute("createSuccess", false);
        model.addAttribute("seatFull", false);
        model.addAttribute("seatOccupied", false);

        Flight savedFlight = flightRepository.findByFromAndToAndDate(flight.getFrom(), flight.getTo(), flight.getDate());

        if (savedFlight != null) {
            if (seatRepository.findAllByFlightId(savedFlight.getFlightId()).size() >= 60) {
                model.addAttribute("seatFull", true);
            } else {
                if (!reservationRepository.existsByUserIdAndFlightIdAndSeatNumber(user.getUserId(), savedFlight.getFlightId(), reservation.getSeatNumber())) {
                    Seat seat = seatRepository.findByFlightIdAndSeatNumber(savedFlight.getFlightId(), reservation.getSeatNumber());
                    if (seat != null && seat.isOccupied()) {
                        model.addAttribute("seatOccupied", true);
                    } else {
                        Reservation newReservation = new Reservation();
                        newReservation.setUserId(user.getUserId());
                        newReservation.setUserName(user.getUsername());
                        newReservation.setFlightId(savedFlight.getFlightId());
                        newReservation.setFrom(savedFlight.getFrom());
                        newReservation.setTo(savedFlight.getTo());
                        newReservation.setDate(savedFlight.getDate());
                        newReservation.setSeatNumber(reservation.getSeatNumber());
                        reservationRepository.save(newReservation);

                        Seat newSeat = new Seat();
                        newSeat.setFlightId(savedFlight.getFlightId());
                        newSeat.setSeatNumber(reservation.getSeatNumber());
                        newSeat.setOccupied(true);
                        seatRepository.save(newSeat);

                        model.addAttribute("createSuccess", true);
                    }
                } else {
                    model.addAttribute("reservationExist", true);
                }
            }
        } else {
            model.addAttribute("flightExist", false);
        }

        return "add_reservation";
    }

    @PostMapping("/reservation/modify_reservation")
    public String modifyReservation(@RequestParam("reservationId") Long reservationId, Model model) {
        Reservation reservation = reservationRepository.findById(reservationId).orElse(null);
        if (reservation == null) return "redirect:/user/reservation/reservations";

        Seat seat = seatRepository.findByFlightIdAndSeatNumber(reservation.getFlightId(), reservation.getSeatNumber());
        model.addAttribute("seat", seat);
        model.addAttribute("selectedReservation", reservation);
        model.addAttribute("modifyReservationSuccess", false);
        model.addAttribute("flightExist", true);
        model.addAttribute("seatOccupied", false);
        return "modify_reservation";
    }

    @PostMapping("/reservation/save_modified_reservation")
    public String saveModifiedReservation(@ModelAttribute("selectedReservation") Reservation reservation, @ModelAttribute("seat") Seat seat1, Model model, HttpSession session) {
        User user = (User) session.getAttribute("user");
        model.addAttribute("modifyReservationSuccess", false);
        model.addAttribute("flightExist", true);
        model.addAttribute("seatOccupied", false);

        Flight flight = flightRepository.findByFromAndToAndDate(reservation.getFrom(), reservation.getTo(), reservation.getDate());
        Reservation savedReservation = reservationRepository.findById(reservation.getReservationId()).orElse(null);

        if (flight != null && savedReservation != null) {
            Seat seat = seatRepository.findByFlightIdAndSeatNumber(seat1.getFlightId(), seat1.getSeatNumber());
            if (reservation.getSeatNumber() != null && seat != null && seat.isOccupied()) {
                model.addAttribute("seatOccupied", true);
            } else {
                savedReservation.setUserId(user.getUserId());
                savedReservation.setUserName(user.getUsername());
                savedReservation.setFlightId(flight.getFlightId());
                savedReservation.setFrom(flight.getFrom());
                savedReservation.setTo(flight.getTo());
                savedReservation.setDate(flight.getDate());
                savedReservation.setSeatNumber(reservation.getSeatNumber());
                reservationRepository.save(savedReservation);

                if (seat != null) {
                    seat.setFlightId(savedReservation.getFlightId());
                    seat.setSeatNumber(reservation.getSeatNumber());
                    seat.setOccupied(true);
                    seatRepository.save(seat);
                }

                model.addAttribute("modifyReservationSuccess", true);
            }
        } else {
            model.addAttribute("flightExist", false);
        }

        model.addAttribute("selectedReservation", reservation);
        return "modify_reservation";
    }

    @PostMapping("/reservation/delete_reservation")
    public String deleteReservation(@RequestParam("reservationId") Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId).orElse(null);
        if (reservation != null) {
            reservationRepository.deleteById(reservation.getReservationId());
            Seat seat = seatRepository.findByFlightIdAndSeatNumber(reservation.getFlightId(), reservation.getSeatNumber());
            if (seat != null) seatRepository.delete(seat);
        }
        return "redirect:user/reservation/reservations";
    }

    @GetMapping("/searchflights")
    public String searchflights(Model model) {
        Date today = new Date();
        List<Flight> flights = flightRepository.findAll();
        List<Flight> flight = flights.stream().filter(f -> !f.getDate().before(today)).collect(Collectors.toList());
        model.addAttribute("flight", flight);
        model.addAttribute("flightobj", new Flight());
        return "search_flights";
    }

    @PostMapping("/searchflights")
    public String searchflightsByDate(@ModelAttribute("flightobj") Flight flight, Model model) {
        List<Flight> flights = flightRepository.findByDate(flight.getDate());
        model.addAttribute("flight", flights);
        return "search_flights";
    }
} 
