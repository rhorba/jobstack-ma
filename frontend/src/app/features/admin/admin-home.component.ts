import { HttpClient } from '@angular/common/http';
import { Component, OnInit, signal } from '@angular/core';

@Component({
  selector: 'app-admin-home',
  standalone: true,
  template: `<div style="padding: 24px">
    <h1>Admin dashboard</h1>
    <p>API says: {{ pingResult() }}</p>
  </div>`,
})
export class AdminHomeComponent implements OnInit {
  readonly pingResult = signal('');

  constructor(private http: HttpClient) {}

  ngOnInit(): void {
    this.http.get('/api/v1/admin/ping', { responseType: 'text' }).subscribe((res) => this.pingResult.set(res));
  }
}
