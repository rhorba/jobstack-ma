import { HttpClient } from '@angular/common/http';
import { Component, OnInit, signal } from '@angular/core';

@Component({
  selector: 'app-candidate-home',
  standalone: true,
  template: `<div style="padding: 24px">
    <h1>Candidate dashboard</h1>
    <p>Logged in as {{ email() }}</p>
  </div>`,
})
export class CandidateHomeComponent implements OnInit {
  readonly email = signal('');

  constructor(private http: HttpClient) {}

  ngOnInit(): void {
    this.http.get<{ email: string }>('/api/v1/candidates/me').subscribe((res) => this.email.set(res.email));
  }
}
